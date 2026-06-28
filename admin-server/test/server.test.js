const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const bcrypt = require('bcrypt');

const { createApp } = require('../src/server');

function requestJson(server, pathName) {
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  return new Promise((resolve, reject) => {
    const req = http.request(
      {
        host: '127.0.0.1',
        port,
        path: pathName,
        method: 'GET',
      },
      (res) => {
        let raw = '';
        res.setEncoding('utf8');
        res.on('data', (chunk) => {
          raw += chunk;
        });
        res.on('end', () => {
          try {
            resolve({
              statusCode: res.statusCode,
              body: JSON.parse(raw),
            });
          } catch (error) {
            reject(error);
          }
        });
      }
    );

    req.on('error', reject);
    req.end();
  });
}

function request(server, pathName, { method = 'GET', headers = {}, body } = {}) {
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  return new Promise((resolve, reject) => {
    const req = http.request(
      {
        host: '127.0.0.1',
        port,
        path: pathName,
        method,
        headers,
      },
      (res) => {
        let raw = '';
        res.setEncoding('utf8');
        res.on('data', (chunk) => {
          raw += chunk;
        });
        res.on('end', () => {
          try {
            resolve({
              statusCode: res.statusCode,
              body: raw ? JSON.parse(raw) : null,
            });
          } catch (error) {
            reject(error);
          }
        });
      }
    );

    req.on('error', reject);
    if (body) {
      req.write(body);
    }
    req.end();
  });
}

test('health endpoint responds with ok=true', async () => {
  const app = createApp({
    db: {
      get: async () => ({ now: '2026-06-28 00:00:00' }),
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await requestJson(server, '/api/health');
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.body, {
      ok: true,
      now: '2026-06-28 00:00:00',
    });
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  }
});

test('protected admin route rejects invalid bearer token', async () => {
  const app = createApp({
    db: {
      get: async () => ({ now: '2026-06-28 00:00:00' }),
      all: async () => [],
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/admin/me', {
      headers: {
        Authorization: 'Bearer not-a-valid-token',
      },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.body, {
      error: 'invalid_token',
    });
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  }
});

test('admin login rejects invalid password for an existing admin', async () => {
  const passwordHash = await bcrypt.hash('CorrectPass123!', 10);
  const app = createApp({
    db: {
      get: async (query) => {
        if (query.includes('FROM admins')) {
          return {
            id: 1,
            username: 'admin',
            password_hash: passwordHash,
          };
        }
        return null;
      },
      all: async () => [],
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/admin/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: 'admin',
        password: 'WrongPass123!',
      }),
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.body, {
      error: 'invalid_credentials',
    });
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  }
});
