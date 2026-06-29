const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const bcrypt = require('bcrypt');
const { open } = require('sqlite');
const sqlite3 = require('sqlite3');

const { resolveBootstrapAdmin } = require('../src/db');

test('resolveBootstrapAdmin rejects default production password', () => {
  assert.throws(
    () =>
      resolveBootstrapAdmin({
        NODE_ENV: 'production',
        ADMIN_BOOTSTRAP_USERNAME: 'admin',
        ADMIN_BOOTSTRAP_PASSWORD: 'ChangeMe123!',
      }),
    /ADMIN_BOOTSTRAP_PASSWORD must be changed/
  );
});

test('resolveBootstrapAdmin trims safe credentials', () => {
  const admin = resolveBootstrapAdmin({
    NODE_ENV: 'production',
    ADMIN_BOOTSTRAP_USERNAME: ' admin-owner ',
    ADMIN_BOOTSTRAP_PASSWORD: ' 0123456789secure ',
  });

  assert.deepEqual(admin, {
    username: 'admin-owner',
    password: '0123456789secure',
  });
});

test('getDb bootstraps required tables into a temp sqlite database', async () => {
  const tempDbFile = path.join(os.tmpdir(), `mohamy-admin-test-${Date.now()}.db`);
  const originalDbFile = process.env.DB_FILE;
  const dbModulePath = require.resolve('../src/db');

  delete require.cache[dbModulePath];
  process.env.DB_FILE = tempDbFile;

  try {
    const { getDb } = require('../src/db');
    const db = await getDb();

    const tables = await db.all(
      "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('admins', 'lawyers', 'licenses', 'devices') ORDER BY name"
    );
    const admin = await db.get('SELECT username, password_hash FROM admins LIMIT 1');
    const passwordMatches = await bcrypt.compare('ChangeMe123!', admin.password_hash);

    await db.close();

    assert.deepEqual(
      tables.map((row) => row.name),
      ['admins', 'devices', 'lawyers', 'licenses']
    );
    assert.equal(admin.username, 'admin');
    assert.equal(passwordMatches, true);
  } finally {
    delete require.cache[dbModulePath];
    if (originalDbFile === undefined) {
      delete process.env.DB_FILE;
    } else {
      process.env.DB_FILE = originalDbFile;
    }

    try {
      if (fs.existsSync(tempDbFile)) {
        fs.rmSync(tempDbFile, { force: true });
      }
      if (fs.existsSync(`${tempDbFile}-wal`)) {
        fs.rmSync(`${tempDbFile}-wal`, { force: true });
      }
      if (fs.existsSync(`${tempDbFile}-shm`)) {
        fs.rmSync(`${tempDbFile}-shm`, { force: true });
      }
    } catch (_error) {
      // Temp cleanup can race with Windows file locking in CI/local runs.
    }
  }
});

test('getDb migrates legacy lawyer/license/device schema for activation compatibility', async () => {
  const tempDbFile = path.join(os.tmpdir(), `mohamy-admin-legacy-${Date.now()}.db`);
  const originalDbFile = process.env.DB_FILE;
  const dbModulePath = require.resolve('../src/db');

  const legacyDb = await open({
    filename: tempDbFile,
    driver: sqlite3.Database,
  });

  await legacyDb.exec(`
    CREATE TABLE admins (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL
    );

    CREATE TABLE lawyers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      username TEXT NOT NULL UNIQUE,
      password TEXT
    );

    CREATE TABLE licenses (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      lawyer_id INTEGER NOT NULL,
      license_key TEXT NOT NULL UNIQUE
    );

    CREATE TABLE devices (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      license_id INTEGER NOT NULL,
      device_id TEXT NOT NULL
    );
  `);

  await legacyDb.run(
    `INSERT INTO lawyers (name, username, password)
     VALUES ('Legacy Lawyer', 'legacy-user', 'LegacyPass123!')`
  );
  await legacyDb.run(
    `INSERT INTO licenses (lawyer_id, license_key)
     VALUES (1, 'LEGACY-LIC-001')`
  );
  await legacyDb.run(
    `INSERT INTO devices (license_id, device_id)
     VALUES (1, 'legacy-device')`
  );
  await legacyDb.close();

  delete require.cache[dbModulePath];
  process.env.DB_FILE = tempDbFile;

  try {
    const { getDb } = require('../src/db');
    const db = await getDb();

    const lawyerColumns = (await db.all(`PRAGMA table_info(lawyers)`)).map((row) => row.name);
    const licenseColumns = (await db.all(`PRAGMA table_info(licenses)`)).map((row) => row.name);
    const deviceColumns = (await db.all(`PRAGMA table_info(devices)`)).map((row) => row.name);
    const migratedLawyer = await db.get(
      `SELECT phone, password_hash, status, notes, updated_at
       FROM lawyers
       WHERE username = 'legacy-user'`
    );

    await db.close();

    assert.ok(lawyerColumns.includes('phone'));
    assert.ok(lawyerColumns.includes('password_hash'));
    assert.ok(lawyerColumns.includes('status'));
    assert.ok(lawyerColumns.includes('notes'));
    assert.ok(lawyerColumns.includes('updated_at'));
    assert.ok(licenseColumns.includes('max_devices'));
    assert.ok(licenseColumns.includes('expires_at'));
    assert.ok(deviceColumns.includes('device_name'));
    assert.ok(deviceColumns.includes('platform'));
    assert.ok(deviceColumns.includes('app_version'));
    assert.ok(deviceColumns.includes('first_activated_at'));
    assert.ok(deviceColumns.includes('last_check_at'));
    assert.ok(deviceColumns.includes('status'));
    assert.equal(migratedLawyer.phone, '');
    assert.equal(migratedLawyer.status, 'active');
    assert.equal(migratedLawyer.notes, '');
    assert.ok(migratedLawyer.updated_at);
    assert.equal(await bcrypt.compare('LegacyPass123!', migratedLawyer.password_hash), true);
  } finally {
    delete require.cache[dbModulePath];
    if (originalDbFile === undefined) {
      delete process.env.DB_FILE;
    } else {
      process.env.DB_FILE = originalDbFile;
    }

    try {
      if (fs.existsSync(tempDbFile)) {
        fs.rmSync(tempDbFile, { force: true });
      }
      if (fs.existsSync(`${tempDbFile}-wal`)) {
        fs.rmSync(`${tempDbFile}-wal`, { force: true });
      }
      if (fs.existsSync(`${tempDbFile}-shm`)) {
        fs.rmSync(`${tempDbFile}-shm`, { force: true });
      }
    } catch (_error) {
      // Temp cleanup can race with Windows file locking in CI/local runs.
    }
  }
});
