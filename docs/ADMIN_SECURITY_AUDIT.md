# Admin Server Security Audit

Date:
- 2026-06-28

Scope:
- `admin-server` npm dependency audit plus controlled compatibility validation for the kept `sqlite3` and `bcrypt` major upgrades.

Historical audit progression:
- Initial baseline before upgrade work: 8 vulnerabilities total (`6 high`, `2 low`)
- After `sqlite3@6.0.1`: 2 vulnerabilities total (`2 high`, `0 low`)
- After `bcrypt@6.0.0`: 0 vulnerabilities total

Current dependency decisions:
- Keep `sqlite3` at `^6.0.1`
- Keep `bcrypt` at `^6.0.0`
- Do not run `npm audit fix --force`

bcrypt upgrade attempted:
- Previous version: `5.1.1`
- Requested version: `6.0.0` via `npm install bcrypt@latest`
- Install result: succeeded
- Node runtime used for compatibility check: `v24.11.0`

Current vulnerability count after bcrypt upgrade:
- Total: 0
- High: 0
- Low: 0

Vulnerable packages:
- None reported by `npm audit`

Direct vs transitive summary:
- No direct audited vulnerable dependency remains in `package.json`
- No transitive audited vulnerability remains in `package-lock.json` according to `npm audit`
- The prior `bcrypt -> @mapbox/node-pre-gyp -> tar` chain is no longer present after the upgrade

Safe fix availability:
- A controlled package-level upgrade resolved the remaining vulnerability chain
- `npm audit fix` and `npm audit fix --force` are not needed in the current state

Would `npm audit fix --force` be risky:
- Yes
- It remains unnecessary and inappropriate for this stabilization workflow because the audit is already clean

Manual recommendation:
- Keep both major dependency upgrades in place
- Recheck native install behavior if the deployment or local runtime moves away from the validated Windows / Node `v24.11.0` environment
- Continue release work separately; dependency cleanup does not change the `1.8.0` release publication block

Compatibility validation performed:
- `npm test` passed before the bcrypt upgrade with 8 tests and 8 passes
- `npm test` passed after the bcrypt upgrade with 9 tests and 9 passes
- Added and passed a temp-database bootstrap assertion that verifies the seeded admin password hash with `bcrypt.compare(...)`
- Added and passed an admin-login invalid-password route test against the current auth helper behavior
- Temporary server smoke test passed against `/api/health` using a temp `DB_FILE`
- Android regression checks remained green after the admin dependency change

Commands run:
- `node -v`
- `npm test`
- `npm audit --json`
- `npm audit`
- `npm outdated`
- `npm install bcrypt@latest`
- `git diff -- admin-server/package.json`
- `git diff -- admin-server/package-lock.json`
- `npm test`
- `npm audit --json`
- `npm audit`
- `Start-Process node ... ; Invoke-RestMethod http://127.0.0.1:18082/api/health`

Current decision:
- Keep `sqlite3@^6.0.1`
- Keep `bcrypt@^6.0.0`
- Do not revert either upgrade
- Do not run any blind audit-force remediation

Remaining risks:
- Native module compatibility was validated only on the current Windows environment with Node `v24.11.0`
- Release/update safety work is still blocked by the missing published `v1.8.0` APK asset, not by the admin dependency state
