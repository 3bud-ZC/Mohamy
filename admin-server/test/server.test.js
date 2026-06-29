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

test('license activation succeeds for an active account using phone login', async () => {
  const passwordHash = await bcrypt.hash('12345678', 10);
  const writes = [];
  const app = createApp({
    db: {
      get: async (query, ...params) => {
        if (query.includes('FROM lawyers')) {
          assert.equal(params[0], '01000000000');
          return {
            id: 7,
            name: 'Test Lawyer',
            phone: '01000000000',
            username: 'test1',
            password_hash: passwordHash,
            status: 'active',
            notes: 'مكتب تجريبي',
          };
        }
        if (query.includes('FROM licenses')) {
          return {
            id: 11,
            lawyer_id: 7,
            license_key: 'LIC-123',
            status: 'active',
            max_devices: 1,
            expires_at: null,
          };
        }
        if (query.includes('FROM devices WHERE license_id = ? AND device_id = ?')) {
          return null;
        }
        return null;
      },
      all: async (query) => {
        if (query.includes('FROM devices WHERE license_id = ? AND status = ?')) {
          return [];
        }
        return [];
      },
      run: async (query, ...params) => {
        writes.push({ query, params });
        return { lastID: 99 };
      },
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/license/activate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: '01000000000',
        password: '12345678',
        device_id: 'device-1',
        device_name: 'Pixel',
        platform: 'android',
        app_version: '1.8.1',
      }),
    });

    assert.equal(response.statusCode, 200);
    assert.equal(response.body.status, 'active');
    assert.equal(response.body.phone, '01000000000');
    assert.equal(response.body.license_key, 'LIC-123');
    assert.equal(typeof response.body.token, 'string');
    assert.ok(writes.some((entry) => entry.query.includes('INSERT INTO devices')));
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  }
});

test('license activation rejects wrong password', async () => {
  const passwordHash = await bcrypt.hash('CorrectPass123!', 10);
  const app = createApp({
    db: {
      get: async (query) => {
        if (query.includes('FROM lawyers')) {
          return {
            id: 7,
            username: 'test1',
            phone: '01000000000',
            password_hash: passwordHash,
            status: 'active',
          };
        }
        return null;
      },
      all: async () => [],
      run: async () => ({ lastID: 1 }),
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/license/activate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: 'test1',
        password: 'WrongPass123!',
        device_id: 'device-1',
      }),
    });

    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.body, {
      error: 'invalid_credentials',
      message: 'اسم المستخدم أو كلمة المرور غير صحيحة.',
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

test('license activation rejects blocked account', async () => {
  const passwordHash = await bcrypt.hash('CorrectPass123!', 10);
  const app = createApp({
    db: {
      get: async (query) => {
        if (query.includes('FROM lawyers')) {
          return {
            id: 7,
            username: 'test1',
            phone: '01000000000',
            password_hash: passwordHash,
            status: 'blocked',
          };
        }
        return null;
      },
      all: async () => [],
      run: async () => ({ lastID: 1 }),
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/license/activate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: 'test1',
        password: 'CorrectPass123!',
        device_id: 'device-1',
      }),
    });

    assert.equal(response.statusCode, 403);
    assert.deepEqual(response.body, {
      error: 'blocked',
      message: 'الحساب غير مفعل.',
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

test('license activation rejects expired license', async () => {
  const passwordHash = await bcrypt.hash('CorrectPass123!', 10);
  const app = createApp({
    db: {
      get: async (query) => {
        if (query.includes('FROM lawyers')) {
          return {
            id: 7,
            username: 'test1',
            phone: '01000000000',
            password_hash: passwordHash,
            status: 'active',
          };
        }
        if (query.includes('FROM licenses')) {
          return {
            id: 11,
            lawyer_id: 7,
            status: 'active',
            max_devices: 1,
            expires_at: '2020-01-01T00:00:00.000Z',
          };
        }
        return null;
      },
      all: async () => [],
      run: async () => ({ lastID: 1 }),
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/license/activate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: 'test1',
        password: 'CorrectPass123!',
        device_id: 'device-1',
      }),
    });

    assert.equal(response.statusCode, 403);
    assert.deepEqual(response.body, {
      error: 'expired',
      message: 'الترخيص منتهي.',
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

test('license activation rejects device-limit overflow', async () => {
  const passwordHash = await bcrypt.hash('CorrectPass123!', 10);
  const app = createApp({
    db: {
      get: async (query) => {
        if (query.includes('FROM lawyers')) {
          return {
            id: 7,
            username: 'test1',
            phone: '01000000000',
            password_hash: passwordHash,
            status: 'active',
          };
        }
        if (query.includes('FROM licenses')) {
          return {
            id: 11,
            lawyer_id: 7,
            status: 'active',
            max_devices: 1,
            expires_at: null,
          };
        }
        if (query.includes('FROM devices WHERE license_id = ? AND device_id = ?')) {
          return null;
        }
        return null;
      },
      all: async (query) => {
        if (query.includes('FROM devices WHERE license_id = ? AND status = ?')) {
          return [{ id: 44, device_id: 'existing-device', status: 'active' }];
        }
        return [];
      },
      run: async () => ({ lastID: 1 }),
    },
    corsOrigins: '',
    trustProxy: false,
  });

  const server = app.listen(0);

  try {
    const response = await request(server, '/api/license/activate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: 'test1',
        password: 'CorrectPass123!',
        device_id: 'new-device',
      }),
    });

    assert.equal(response.statusCode, 409);
    assert.deepEqual(response.body, {
      error: 'device_limit_reached',
      message: 'تم تجاوز عدد الأجهزة المسموح بها.',
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
