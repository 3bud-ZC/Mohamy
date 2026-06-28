const test = require('node:test');
const assert = require('node:assert/strict');

const { createAuthHelpers, resolveJwtSecret } = require('../src/auth');

test('resolveJwtSecret rejects weak production secret', () => {
  assert.throws(
    () => resolveJwtSecret({ NODE_ENV: 'production', JWT_SECRET: 'short' }, () => {}),
    /JWT_SECRET must be set to a strong value/
  );
});

test('resolveJwtSecret uses local fallback outside production', () => {
  assert.equal(
    resolveJwtSecret({ NODE_ENV: 'test', JWT_SECRET: '' }, () => {}),
    'local-dev-secret-change-me'
  );
});

test('createAuthHelpers signs and verifies admin tokens', () => {
  const auth = createAuthHelpers('abcdefghijklmnopqrstuvwxyz123456');
  const token = auth.signAdminToken({ type: 'admin', username: 'admin' });
  const payload = auth.verifyToken(token);

  assert.equal(payload.type, 'admin');
  assert.equal(payload.username, 'admin');
});
