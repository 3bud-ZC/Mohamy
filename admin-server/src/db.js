const path = require('path');
const bcrypt = require('bcrypt');
const { open } = require('sqlite');
const sqlite3 = require('sqlite3');

const DB_FILE = process.env.DB_FILE || path.join(__dirname, '..', 'data', 'admin.db');

function resolveBootstrapAdmin() {
  const username = (process.env.ADMIN_BOOTSTRAP_USERNAME || 'admin').trim();
  const password = (process.env.ADMIN_BOOTSTRAP_PASSWORD || 'ChangeMe123!').trim();

  if (process.env.NODE_ENV === 'production') {
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

  await db.run('UPDATE licenses SET max_devices = 1, expires_at = NULL WHERE max_devices IS NULL OR max_devices <> 1 OR expires_at IS NOT NULL');

  const bootstrapAdmin = resolveBootstrapAdmin();
  const admin = await db.get('SELECT id FROM admins WHERE username = ?', bootstrapAdmin.username);
  if (!admin) {
    const hash = await bcrypt.hash(bootstrapAdmin.password, 10);
    await db.run('INSERT INTO admins (username, password_hash) VALUES (?, ?)', bootstrapAdmin.username, hash);
  }

  return db;
}

module.exports = { getDb };
