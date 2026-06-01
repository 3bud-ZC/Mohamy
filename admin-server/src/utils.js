const crypto = require('crypto');

function generateLicenseKey() {
  const part = () => crypto.randomBytes(3).toString('hex').toUpperCase();
  return `MP-${part()}-${part()}-${part()}`;
}

function nowIso() {
  return new Date().toISOString();
}

module.exports = { generateLicenseKey, nowIso };
