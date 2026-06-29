const path = require('path');
const bcrypt = require('bcrypt');
const { open } = require('sqlite');
const sqlite3 = require('sqlite3');

const DB_FILE = process.env.DB_FILE || path.join(__dirname, '..', 'data', 'admin.db');

async function getTableColumns(db, tableName) {
  const rows = await db.all(`PRAGMA table_info(${tableName})`);
  return rows.map((row) => row.name);
}

async function addColumnIfMissing(db, tableName, columnName, sqlDefinition) {
  const columns = await getTableColumns(db, tableName);
  if (columns.includes(columnName)) return;
  await db.exec(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${sqlDefinition}`);
}

async function ensureCompatibilitySchema(db) {
  await addColumnIfMissing(db, 'admins', 'created_at', 'TEXT');

  await addColumnIfMissing(db, 'lawyers', 'phone', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'lawyers', 'password_hash', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'lawyers', 'status', "TEXT NOT NULL DEFAULT 'active'");
  await addColumnIfMissing(db, 'lawyers', 'notes', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'lawyers', 'created_at', 'TEXT');
  await addColumnIfMissing(db, 'lawyers', 'updated_at', 'TEXT');

  await addColumnIfMissing(db, 'licenses', 'status', "TEXT NOT NULL DEFAULT 'active'");
  await addColumnIfMissing(db, 'licenses', 'max_devices', 'INTEGER NOT NULL DEFAULT 1');
  await addColumnIfMissing(db, 'licenses', 'expires_at', 'TEXT');
  await addColumnIfMissing(db, 'licenses', 'created_at', 'TEXT');
  await addColumnIfMissing(db, 'licenses', 'updated_at', 'TEXT');

  await addColumnIfMissing(db, 'devices', 'device_name', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'devices', 'platform', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'devices', 'app_version', "TEXT DEFAULT ''");
  await addColumnIfMissing(db, 'devices', 'first_activated_at', 'TEXT');
  await addColumnIfMissing(db, 'devices', 'last_check_at', 'TEXT');
  await addColumnIfMissing(db, 'devices', 'status', "TEXT NOT NULL DEFAULT 'active'");

  const lawyerColumns = await getTableColumns(db, 'lawyers');
  if (lawyerColumns.includes('password')) {
    const legacyRows = await db.all(
      `SELECT id, password
       FROM lawyers
       WHERE COALESCE(password_hash, '') = ''
         AND COALESCE(password, '') <> ''`
    );
    for (const row of legacyRows) {
      const hash = await bcrypt.hash(String(row.password), 10);
      await db.run(
        `UPDATE lawyers
         SET password_hash = ?, updated_at = datetime('now')
         WHERE id = ?`,
        hash,
        row.id
      );
    }
  }

  await db.run(
    `UPDATE lawyers
     SET phone = COALESCE(phone, ''),
         status = CASE
           WHEN status IS NULL OR TRIM(status) = '' THEN 'active'
           ELSE status
         END,
         notes = COALESCE(notes, ''),
         created_at = COALESCE(created_at, datetime('now')),
         updated_at = COALESCE(updated_at, datetime('now'))`
  );

  await db.run(
    `UPDATE licenses
     SET status = CASE
       WHEN status IS NULL OR TRIM(status) = '' THEN 'active'
       ELSE status
     END,
         max_devices = 1,
         expires_at = NULL,
         created_at = COALESCE(created_at, datetime('now')),
         updated_at = COALESCE(updated_at, datetime('now'))`
  );

  await db.run(
    `UPDATE devices
     SET device_name = COALESCE(device_name, ''),
         platform = COALESCE(platform, ''),
         app_version = COALESCE(app_version, ''),
         first_activated_at = COALESCE(first_activated_at, datetime('now')),
         last_check_at = COALESCE(last_check_at, datetime('now')),
         status = CASE
           WHEN status IS NULL OR TRIM(status) = '' THEN 'active'
           ELSE status
         END`
  );
}

function resolveBootstrapAdmin(env = process.env) {
  const username = (env.ADMIN_BOOTSTRAP_USERNAME || 'admin').trim();
  const password = (env.ADMIN_BOOTSTRAP_PASSWORD || 'ChangeMe123!').trim();

  if (env.NODE_ENV === 'production') {
    if (!username) {
      throw new Error('ADMIN_BOOTSTRAP_USERNAME cannot be empty in production.');
    }
    if (password.length < 10 || password === 'ChangeMe123!') {
      throw new Error('ADMIN_BOOTSTRAP_PASSWORD must be changed in production (min 10 chars).');
    }
  }

  return { username, password };
}

async function getDb() {
  const db = await open({
    filename: DB_FILE,
    driver: sqlite3.Database,
  });

  await db.exec(`
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS admins (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS lawyers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      phone TEXT,
      username TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'active',
      notes TEXT DEFAULT '',
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS licenses (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      lawyer_id INTEGER NOT NULL,
      license_key TEXT NOT NULL UNIQUE,
      status TEXT NOT NULL DEFAULT 'active',
      max_devices INTEGER NOT NULL DEFAULT 1,
      expires_at TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS devices (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      license_id INTEGER NOT NULL,
      device_id TEXT NOT NULL,
      device_name TEXT,
      platform TEXT,
      app_version TEXT,
      first_activated_at TEXT NOT NULL DEFAULT (datetime('now')),
      last_check_at TEXT NOT NULL DEFAULT (datetime('now')),
      status TEXT NOT NULL DEFAULT 'active',
      UNIQUE(license_id, device_id),
      FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
    );
  `);

  await ensureCompatibilitySchema(db);

  const bootstrapAdmin = resolveBootstrapAdmin();
  const admin = await db.get('SELECT id FROM admins WHERE username = ?', bootstrapAdmin.username);
  if (!admin) {
    const hash = await bcrypt.hash(bootstrapAdmin.password, 10);
    await db.run('INSERT INTO admins (username, password_hash) VALUES (?, ?)', bootstrapAdmin.username, hash);
  }

  return db;
}

module.exports = { getDb, resolveBootstrapAdmin };
