# ACTIVATION / ADMIN INTEGRATION QA - 2026-06-29

## Scope
- Repository: `C:\Users\Abud\Desktop\GitHub\MohamyPhone`
- Goal: verify Android activation contract, production deployment on the new VPS, admin/license linkage, and manual APK readiness without touching signing assets, `applicationId`, or `update/latest.json`.

## URLs Checked
- Admin root: `https://mohamy.abud.fun`
- API base: `https://mohamy.abud.fun/api`
- Published release APK: `https://github.com/3bud-ZC/Mohamy/releases/download/v1.8.1/app-release.apk`

## Android Contract (Verified From Code)
- Base URL: `https://mohamy.abud.fun`
- Activation endpoint: `POST /api/license/activate`
- Request JSON:
  - `username`
  - `password`
  - `device_id`
  - `device_name`
  - `platform`
  - `app_version`
- Response JSON consumed by Android:
  - `token`
  - `expires_at`
  - `lawyer_name`
  - `office_name`
  - `phone`
  - `license_key`

## Local Validation
- `npm test` -> pass (`15/15`)
- `npm audit` -> pass (`0 vulnerabilities`)
- `.\gradlew.bat --% clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace` -> **BUILD SUCCESSFUL**
- Debug APK path:
  - `C:\Users\Abud\Desktop\GitHub\MohamyPhone\app\build\outputs\apk\debug\app-debug.apk`

## Production Access Check
- Old VPS was not used:
  - `161.35.54.6` is deleted
- New VPS used:
  - public IP: `167.99.157.6`
  - SSH user: `root`
  - domain kept unchanged: `mohamy.abud.fun`
- Direct production SSH inspection on `2026-06-29` succeeded.
- Server baseline:
  - OS: `Ubuntu 24.04.3 LTS`
  - Nginx: `1.24.0`
  - Node.js: `20.20.2`
  - PM2: `7.0.1`

## Production Deployment State
- Deployment path:
  - `/var/www/mohamy-phone-admin`
- Current symlink target:
  - `/var/www/mohamy-phone-admin/releases/20260629204100`
- Backup path created before change:
  - `/var/www/mohamy-phone-admin/backups/20260629203820`
- PM2 process:
  - name: `mohamy-phone-admin`
  - status: `online`
  - exec cwd: `/var/www/mohamy-phone-admin/releases/20260629204100/admin-server`
- Internal app port:
  - `3130`
- Nginx site path:
  - `/etc/nginx/sites-available/mohamy.abud.fun`
- SSL/domain status:
  - `https://mohamy.abud.fun` active
  - Certbot-managed certificate still present and valid for the domain
- Production env/data preservation:
  - `.env` reused from `/var/www/mohamy-phone-admin/shared/.env`
  - SQLite DB preserved at `/var/www/mohamy-phone-admin/shared/data/license.sqlite`

## Admin/API Route Checks
- `GET /` -> `200 OK`
- `GET /api/health` -> `200 OK`
- `nginx -t` -> success

## Invalid Activation Probe
- Probe route:
  - `POST https://mohamy.abud.fun/api/license/activate`
- Payload type:
  - harmless invalid credentials only
- Result after redeploy:
  - HTTP `401`
  - controlled Arabic invalid-credentials message
- Conclusion:
  - the previous production activation failure is resolved
  - invalid credentials no longer trigger `HTTP 500`

## Live Test Lawyer Account
- Account prepared on production:
  - name: `Test Lawyer`
  - username: `test1`
  - phone: `01000000000`
  - password: `123456`
  - status: `active`
  - license status: `active`
  - max devices: `1`
  - expiry: empty / no expiry

## Live Activation Verification
- Verification route:
  - `POST https://mohamy.abud.fun/api/license/activate`
- Payload:
  - `username: test1`
  - `password: 123456`
  - `device_id: manual-test-device-001`
  - `device_name: BlueStacks Manual Test`
  - `platform: android`
  - `app_version: 1.8.1-debug`
- Result:
  - HTTP `200`
  - token returned
  - `lawyer_name` returned
  - license fields returned
- Confirmed payload shape:
  - no cases/files/tasks are sent to the server by this activation route

## APK Install Commands For Manual Test
- Normal reinstall:
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- If an old conflicting install exists:
  - `adb uninstall com.aistudio.mohamyphone.lylawar`
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Manual Test Credentials
- username: `test1`
- password: `123456`

## Pass / Fail Conclusion
- Repo activation/admin fix: **PASS**
- Production redeploy on the new VPS: **PASS**
- Production invalid activation handling: **PASS**
- Production live test activation: **PASS**
- Android build + debug APK generation: **PASS**
- Manual BlueStacks / real-device post-login smoke: **PENDING**

## Remaining Work
- Install the rebuilt debug APK on BlueStacks or a real device and verify the live login with `test1 / 123456`.
- Capture the final success screenshot set for:
  - dashboard
  - cases
  - clients
  - sessions
  - tasks
  - files
  - settings
- Refresh `admin-server/package-lock.json` before the next VPS deploy because the VPS required `npm install --omit=dev` fallback after `npm ci` failed on lockfile drift.
- Watch VPS disk usage:
  - root filesystem has about `3.8G` free (`94%` used)
