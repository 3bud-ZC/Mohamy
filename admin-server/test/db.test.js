const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const bcrypt = require('bcrypt');

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

    if (fs.existsSync(tempDbFile)) {
      fs.rmSync(tempDbFile, { force: true });
    }
  }
});
