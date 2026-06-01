const jwt = require('jsonwebtoken');

function resolveJwtSecret() {
  const provided = (process.env.JWT_SECRET || '').trim();
  if (provided.length >= 24) {
    return provided;
  }

  const fallback = 'local-dev-secret-change-me';
  if (process.env.NODE_ENV === 'production') {
    throw new Error('JWT_SECRET must be set to a strong value (min 24 chars) in production.');
  }

  console.warn('[auth] JWT_SECRET is missing/weak. Using local development fallback secret.');
  return fallback;
}

const JWT_SECRET = resolveJwtSecret();

function signAdminToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '12h' });
}

function signLicenseToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '30d' });
}

function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

function requireAdmin(req, res, next) {
  const auth = req.headers.authorization || '';
  const token = auth.startsWith('Bearer ') ? auth.slice(7) : '';
  if (!token) {
    return res.status(401).json({ error: 'missing_token' });
  }
  try {
    const payload = verifyToken(token);
    if (payload.type !== 'admin') {
      return res.status(403).json({ error: 'forbidden' });
    }
    req.admin = payload;
    return next();
  } catch (_err) {
    return res.status(401).json({ error: 'invalid_token' });
  }
}

module.exports = {
  signAdminToken,
  signLicenseToken,
  verifyToken,
  requireAdmin,
};
