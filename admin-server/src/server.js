require('dotenv').config();

const path = require('path');
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const { getDb } = require('./db');
const { signAdminToken, signLicenseToken, requireAdmin, verifyToken } = require('./auth');
const { generateLicenseKey } = require('./utils');

const PORT = Number(process.env.PORT || 8080);
const LIFETIME_MAX_DEVICES = 1;

function parseCorsOrigins(rawValue = process.env.CORS_ORIGINS) {
  const raw = (rawValue || '').trim();
  if (!raw) return true;

  const origins = raw
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean);

  if (origins.includes('*')) return true;

  return function allowOrigin(origin, callback) {
    if (!origin || origins.includes(origin)) {
      callback(null, true);
      return;
    }
    callback(new Error('origin_not_allowed'));
  };
}

function normalizeStatus(input, allowed, fallback) {
  const value = String(input || fallback).trim().toLowerCase();
  if (!allowed.includes(value)) return fallback;
  return value;
}

async function findLawyerByIdentity(db, rawIdentity) {
  const identity = String(rawIdentity || '').trim();
  if (!identity) return null;
  return db.get(
    `SELECT *
     FROM lawyers
     WHERE username = ? OR phone = ?
     ORDER BY CASE
       WHEN username = ? THEN 0
       WHEN phone = ? THEN 1
       ELSE 2
     END
     LIMIT 1`,
    identity,
    identity,
    identity,
    identity
  );
}

function createApp({
  db,
  corsOrigins = process.env.CORS_ORIGINS,
  trustProxy = process.env.TRUST_PROXY === 'true',
} = {}) {
  if (!db) {
    throw new Error('db is required');
  }

  const app = express();

  app.set('trust proxy', trustProxy);
  app.use(cors({ origin: parseCorsOrigins(corsOrigins) }));
  app.use(express.json({ limit: '1mb' }));
  app.use(express.static(path.join(__dirname, '..', 'public')));

  app.get('/api/health', async (_req, res) => {
    const row = await db.get("SELECT datetime('now') AS now");
    res.json({ ok: true, now: row.now });
  });

  app.post('/api/admin/login', async (req, res) => {
    const { username, password } = req.body || {};
    if (!username || !password) {
      return res.status(400).json({ error: 'username_password_required' });
    }
    const admin = await db.get('SELECT * FROM admins WHERE username = ?', username.trim());
    if (!admin) {
      return res.status(401).json({ error: 'invalid_credentials' });
    }
    const valid = await bcrypt.compare(password, admin.password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'invalid_credentials' });
    }
    const token = signAdminToken({ type: 'admin', admin_id: admin.id, username: admin.username });
    return res.json({ token, username: admin.username });
  });

  app.get('/api/admin/me', requireAdmin, async (req, res) => {
    return res.json({ ok: true, username: req.admin.username });
  });

  app.get('/api/admin/lawyers', requireAdmin, async (_req, res) => {
    const rows = await db.all(`
      SELECT
        l.id,
        l.name,
        l.phone,
        l.username,
        l.status,
        l.notes,
        l.created_at,
        l.updated_at,
        lic.id AS license_id,
        lic.license_key,
        lic.status AS license_status,
        lic.max_devices,
        lic.expires_at,
        lic.updated_at AS license_updated_at
      FROM lawyers l
      LEFT JOIN licenses lic ON lic.lawyer_id = l.id
      ORDER BY l.created_at DESC
    `);

    const grouped = [];
    for (const row of rows) {
      const devices = row.license_id
        ? await db.all(
            `SELECT id, device_id, device_name, platform, app_version, first_activated_at, last_check_at, status
             FROM devices WHERE license_id = ? ORDER BY first_activated_at DESC`,
            row.license_id
          )
        : [];
      grouped.push({
        id: row.id,
        name: row.name,
        phone: row.phone,
        username: row.username,
        status: row.status,
        notes: row.notes,
        created_at: row.created_at,
        updated_at: row.updated_at,
        license: row.license_id
          ? {
              id: row.license_id,
              license_key: row.license_key,
              status: row.license_status,
              max_devices: row.max_devices,
              expires_at: row.expires_at,
              updated_at: row.license_updated_at,
              devices,
            }
          : null,
      });
    }

    return res.json({ lawyers: grouped });
  });

  app.post('/api/admin/lawyers', requireAdmin, async (req, res) => {
    const {
      name,
      phone = '',
      username,
      password,
      status = 'active',
      notes = '',
      license_status = 'active',
    } = req.body || {};

    if (!name || !username || !password) {
      return res.status(400).json({ error: 'name_username_password_required' });
    }

    const allowedStatus = ['active', 'blocked', 'inactive'];
    if (!allowedStatus.includes(status)) {
      return res.status(400).json({ error: 'invalid_status' });
    }

    const normalizedLicenseStatus = normalizeStatus(license_status, ['active', 'blocked', 'inactive'], 'active');
    const hash = await bcrypt.hash(password, 10);
    const licenseKey = generateLicenseKey();

    try {
      const insertLawyer = await db.run(
        `INSERT INTO lawyers (name, phone, username, password_hash, status, notes, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))`,
        name.trim(),
        phone.trim(),
        username.trim(),
        hash,
        status,
        notes
      );
      const lawyerId = insertLawyer.lastID;
      await db.run(
        `INSERT INTO licenses (lawyer_id, license_key, status, max_devices, expires_at, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'))`,
        lawyerId,
        licenseKey,
        normalizedLicenseStatus,
        LIFETIME_MAX_DEVICES,
        null
      );
      return res.json({ ok: true, lawyer_id: lawyerId, license_key: licenseKey });
    } catch (err) {
      if (String(err.message || '').includes('UNIQUE')) {
        return res.status(409).json({ error: 'username_or_license_exists' });
      }
      return res.status(500).json({ error: 'failed_to_create_lawyer' });
    }
  });

  app.patch('/api/admin/lawyers/:id', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const lawyer = await db.get('SELECT * FROM lawyers WHERE id = ?', id);
    if (!lawyer) return res.status(404).json({ error: 'lawyer_not_found' });

    const name = (req.body?.name ?? lawyer.name).trim();
    const phone = (req.body?.phone ?? lawyer.phone ?? '').trim();
    const username = (req.body?.username ?? lawyer.username).trim();
    const notes = req.body?.notes ?? lawyer.notes ?? '';

    try {
      await db.run(
        `UPDATE lawyers
         SET name = ?, phone = ?, username = ?, notes = ?, updated_at = datetime('now')
         WHERE id = ?`,
        name,
        phone,
        username,
        notes,
        id
      );
      return res.json({ ok: true });
    } catch (err) {
      if (String(err.message || '').includes('UNIQUE')) {
        return res.status(409).json({ error: 'username_exists' });
      }
      return res.status(500).json({ error: 'failed_to_update_lawyer' });
    }
  });

  app.patch('/api/admin/lawyers/:id/status', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const status = String(req.body?.status || '').trim();
    const allowed = ['active', 'blocked', 'inactive'];
    if (!allowed.includes(status)) {
      return res.status(400).json({ error: 'invalid_status' });
    }

    const lawyer = await db.get('SELECT * FROM lawyers WHERE id = ?', id);
    if (!lawyer) return res.status(404).json({ error: 'lawyer_not_found' });

    await db.run("UPDATE lawyers SET status = ?, updated_at = datetime('now') WHERE id = ?", status, id);
    await db.run(
      "UPDATE licenses SET status = ?, updated_at = datetime('now') WHERE lawyer_id = ?",
      status === 'active' ? 'active' : 'blocked',
      id
    );

    return res.json({ ok: true });
  });

  app.patch('/api/admin/licenses/:id', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const license = await db.get('SELECT * FROM licenses WHERE id = ?', id);
    if (!license) return res.status(404).json({ error: 'license_not_found' });

    const status = normalizeStatus(req.body?.status, ['active', 'blocked', 'inactive'], license.status);
    const maxDevices = LIFETIME_MAX_DEVICES;
    const expiresAt = null;

    await db.run(
      `UPDATE licenses
       SET status = ?, max_devices = ?, expires_at = ?, updated_at = datetime('now')
       WHERE id = ?`,
      status,
      maxDevices,
      expiresAt,
      id
    );

    if (status !== 'active') {
      await db.run("UPDATE devices SET status = 'inactive' WHERE license_id = ?", id);
    }

    return res.json({ ok: true });
  });

  app.delete('/api/admin/lawyers/:id', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const lawyer = await db.get('SELECT * FROM lawyers WHERE id = ?', id);
    if (!lawyer) return res.status(404).json({ error: 'lawyer_not_found' });

    await db.run('DELETE FROM lawyers WHERE id = ?', id);
    return res.json({ ok: true });
  });

  app.post('/api/admin/lawyers/:id/reset-password', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const newPassword = String(req.body?.new_password || '').trim();
    if (!newPassword || newPassword.length < 8) {
      return res.status(400).json({ error: 'new_password_min_8' });
    }
    const lawyer = await db.get('SELECT * FROM lawyers WHERE id = ?', id);
    if (!lawyer) return res.status(404).json({ error: 'lawyer_not_found' });

    const hash = await bcrypt.hash(newPassword, 10);
    await db.run("UPDATE lawyers SET password_hash = ?, updated_at = datetime('now') WHERE id = ?", hash, id);
    return res.json({ ok: true });
  });

  app.post('/api/admin/licenses/:id/reset-device', requireAdmin, async (req, res) => {
    const id = Number(req.params.id);
    const license = await db.get('SELECT * FROM licenses WHERE id = ?', id);
    if (!license) return res.status(404).json({ error: 'license_not_found' });

    await db.run('DELETE FROM devices WHERE license_id = ?', id);
    await db.run("UPDATE licenses SET updated_at = datetime('now') WHERE id = ?", id);

    return res.json({ ok: true });
  });

  app.post('/api/license/activate', async (req, res) => {
    const {
      username,
      password,
      device_id,
      device_name = '',
      platform = 'android',
      app_version = '',
    } = req.body || {};

    if (!username || !password || !device_id) {
      return res.status(400).json({ error: 'username_password_device_required' });
    }

    const identity = String(username).trim();
    const lawyer = await findLawyerByIdentity(db, identity);
    if (!lawyer) {
      return res.status(401).json({
        error: 'invalid_credentials',
        message: 'اسم المستخدم أو كلمة المرور غير صحيحة.',
      });
    }

    let passOk = false;
    if (typeof lawyer.password_hash === 'string' && lawyer.password_hash.trim()) {
      passOk = await bcrypt.compare(String(password), lawyer.password_hash);
    } else if (typeof lawyer.password === 'string' && lawyer.password.length > 0) {
      passOk = String(password) === lawyer.password;
      if (passOk) {
        const upgradedHash = await bcrypt.hash(String(password), 10);
        await db.run(
          "UPDATE lawyers SET password_hash = ?, updated_at = datetime('now') WHERE id = ?",
          upgradedHash,
          lawyer.id
        );
      }
    }
    if (!passOk) {
      return res.status(401).json({
        error: 'invalid_credentials',
        message: 'اسم المستخدم أو كلمة المرور غير صحيحة.',
      });
    }

    if (lawyer.status === 'blocked') {
      return res.status(403).json({ error: 'blocked', message: 'الحساب غير مفعل.' });
    }
    if (lawyer.status === 'inactive') {
      return res.status(403).json({ error: 'inactive', message: 'الحساب غير مفعل.' });
    }

    const license = await db.get('SELECT * FROM licenses WHERE lawyer_id = ? ORDER BY id DESC LIMIT 1', lawyer.id);
    if (!license) {
      return res.status(403).json({ error: 'no_license', message: 'لا يوجد ترخيص مرتبط بالحساب.' });
    }

    if (license.status === 'blocked' || license.status === 'inactive') {
      return res.status(403).json({ error: 'inactive', message: 'الحساب غير مفعل.' });
    }

    if (license.expires_at) {
      const expired = new Date(license.expires_at).getTime() < Date.now();
      if (expired) {
        return res.status(403).json({ error: 'expired', message: 'الترخيص منتهي.' });
      }
    }

    const currentDevice = await db.get(
      'SELECT * FROM devices WHERE license_id = ? AND device_id = ?',
      license.id,
      String(device_id)
    );
    const allActiveDevices = await db.all('SELECT * FROM devices WHERE license_id = ? AND status = ?', license.id, 'active');

    if (!currentDevice && allActiveDevices.length >= (license.max_devices || 1)) {
      return res.status(409).json({
        error: 'device_limit_reached',
        message: 'تم تجاوز عدد الأجهزة المسموح بها.',
      });
    }

    if (currentDevice) {
      await db.run(
        `UPDATE devices
         SET device_name = ?, platform = ?, app_version = ?, last_check_at = datetime('now'), status = 'active'
         WHERE id = ?`,
        String(device_name),
        String(platform),
        String(app_version),
        currentDevice.id
      );
    } else {
      await db.run(
        `INSERT INTO devices
         (license_id, device_id, device_name, platform, app_version, first_activated_at, last_check_at, status)
         VALUES (?, ?, ?, ?, ?, datetime('now'), datetime('now'), 'active')`,
        license.id,
        String(device_id),
        String(device_name),
        String(platform),
        String(app_version)
      );
    }

    await db.run("UPDATE licenses SET updated_at = datetime('now') WHERE id = ?", license.id);

    const token = signLicenseToken({
      type: 'license',
      lawyer_id: lawyer.id,
      license_id: license.id,
      username: lawyer.username,
      device_id: String(device_id),
    });

    return res.json({
      status: 'active',
      token,
      lawyer_name: lawyer.name,
      office_name: lawyer.notes || 'مكتب محاماة',
      phone: lawyer.phone || '',
      license_key: license.license_key,
      max_devices: license.max_devices,
      expires_at: license.expires_at,
    });
  });

  app.post('/api/license/check', async (req, res) => {
    const { token, device_id, app_version = '' } = req.body || {};
    if (!token || !device_id) {
      return res.status(400).json({ error: 'token_device_required' });
    }

    let payload;
    try {
      payload = verifyToken(String(token));
    } catch (_err) {
      return res.status(401).json({ status: 'invalid', error: 'invalid_token' });
    }

    if (payload.type !== 'license') {
      return res.status(401).json({ status: 'invalid', error: 'invalid_token_type' });
    }

    const license = await db.get('SELECT * FROM licenses WHERE id = ?', payload.license_id);
    if (!license) return res.status(404).json({ status: 'invalid', error: 'license_not_found' });

    const lawyer = await db.get('SELECT * FROM lawyers WHERE id = ?', payload.lawyer_id);
    if (!lawyer) return res.status(404).json({ status: 'invalid', error: 'lawyer_not_found' });

    if (lawyer.status === 'blocked' || license.status === 'blocked') {
      return res.status(403).json({ status: 'blocked', message: 'الحساب موقوف.' });
    }

    if (license.expires_at && new Date(license.expires_at).getTime() < Date.now()) {
      return res.status(403).json({ status: 'expired', message: 'الترخيص منتهي.' });
    }

    const device = await db.get(
      'SELECT * FROM devices WHERE license_id = ? AND device_id = ? AND status = ?',
      license.id,
      String(device_id),
      'active'
    );
    if (!device) {
      return res.status(409).json({ status: 'device_mismatch', message: 'الحساب مرتبط بجهاز آخر.' });
    }

    await db.run(
      "UPDATE devices SET last_check_at = datetime('now'), app_version = ? WHERE id = ?",
      String(app_version),
      device.id
    );
    await db.run("UPDATE licenses SET updated_at = datetime('now') WHERE id = ?", license.id);

    return res.json({
      status: 'active',
      lawyer_status: lawyer.status,
      license_status: license.status,
      expires_at: license.expires_at,
      last_check_at: new Date().toISOString(),
    });
  });

  app.use((err, _req, res, _next) => {
    if (err && err.message === 'origin_not_allowed') {
      return res.status(403).json({ error: 'origin_not_allowed' });
    }
    console.error('Unhandled server error:', err);
    return res.status(500).json({ error: 'internal_server_error' });
  });

  return app;
}

async function start({ port = PORT } = {}) {
  const db = await getDb();
  const app = createApp({ db });
  const server = app.listen(port, () => {
    console.log(`Admin server running on http://localhost:${port}`);
  });
  return { app, db, server };
}

if (require.main === module) {
  start().catch((err) => {
    console.error('Failed to start admin-server:', err);
    process.exit(1);
  });
}

module.exports = {
  createApp,
  normalizeStatus,
  parseCorsOrigins,
  start,
};
