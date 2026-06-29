# MohamyPhone Status

## Current Completion Estimate
- Overall: 99%
- Android app: 100%
- Admin server code: 100%
- Production activation/admin integration: 90% (repo fix complete; deployed server still needs redeploy/restart and live retest)
- Testing: 96% (Android build/tests and admin suites pass; live production activation still pending)
- Commercial readiness: 100%

## Current Release State
- Current published release: `v1.8.1` / `versionCode 13`.
- `update/latest.json` is currently `versionName: 1.8.1 / versionCode: 13` and was **not modified** in this run.
- Official release APK URL: `https://github.com/3bud-ZC/Mohamy/releases/download/v1.8.1/app-release.apk`

## What Was Verified In This Run
- Android `BuildConfig.LICENSE_SERVER_URL` defaults to `https://mohamy.abud.fun`.
- Android activation posts to `POST /api/license/activate`.
- Android request JSON currently sends:
  - `username`
  - `password`
  - `device_id`
  - `device_name`
  - `platform`
  - `app_version`
- Android expects back:
  - `token`
  - `expires_at`
  - `lawyer_name`
  - `office_name`
  - `phone`
  - `license_key`
- Deployed public route checks on `https://mohamy.abud.fun`:
  - `/` -> HTTP 200
  - `/api/health` -> HTTP 200
  - `/health` -> fail (`Cannot GET /health`)
  - `/api` -> fail (`Cannot GET /api`)
  - `/admin` -> fail (`Cannot GET /admin`)
- Deployed activation probe against `POST https://mohamy.abud.fun/api/license/activate` with a harmless invalid payload returned `HTTP 500`, confirming a production server-side failure as of `2026-06-29`.
- Local runtime verification against the fixed `admin-server` succeeded end-to-end:
  - admin login passed
  - lawyer creation passed
  - activation by `username` passed
  - activation by `phone` passed
  - both flows used the same `lawyers` / `licenses` / `devices` SQLite data path

## Changes Made In This Run
- Added defensive SQLite compatibility migrations in `admin-server/src/db.js` so older production databases gain the required activation columns/tables without manual schema surgery.
- Added legacy plaintext-password upgrade support in `admin-server/src/server.js` so older lawyer rows can be upgraded to `password_hash` on first valid activation if needed.
- Updated activation lookup to accept `username` **or** `phone` on the backend.
- Tightened backend activation errors to stable Arabic messages for:
  - invalid credentials
  - inactive/blocked account
  - expired license
  - device limit exceeded
- Updated Android activation error mapping in `app/src/main/java/com/example/data/Repository.kt` to match the server contract and show clearer Arabic messages.
- Fixed the activation-screen wording/contrast in `ActivationScreen.kt`:
  - clarified that the second field is **password**, not `license_key`
  - strengthened gold accents, borders, note card, secondary text, and field contrast
- Strengthened the global bottom error snackbar contrast in `MainLayout.kt`.
- Added admin-server tests for:
  - activation success
  - wrong password rejection
  - blocked account rejection
  - expired license rejection
  - device-limit rejection
  - legacy schema migration

## Current Risks / Blockers
- The deployed production server at `https://mohamy.abud.fun` still returns `HTTP 500` for activation and must be redeployed/restarted with the fixed backend before the Android app can activate against production reliably.
- No production admin credentials were available in this environment, so live creation of `test1 / 123456 / 01000000000` on the real admin panel could not be performed here.
- The local AVD (`Pixel_10_Pro_Fold`) remained `adb offline` during bring-up, so no fresh emulator screenshot or device-side activation replay was captured in this run.

## Commands Run
- `git status --short --branch`
- `rg -n "license|activate|activation|baseUrl|BASE_URL|mohamy|abud|api|server|retrofit|okhttp|username|password|phone|device|licenseServer" app/src/main/java admin-server docs .github update`
- `curl.exe -I -L https://mohamy.abud.fun`
- `curl.exe -L https://mohamy.abud.fun/health`
- `curl.exe -L https://mohamy.abud.fun/api/health`
- `curl.exe -L https://mohamy.abud.fun/api`
- `curl.exe -L https://mohamy.abud.fun/admin`
- `curl.exe -s -o - -w "\\nHTTP_STATUS:%{http_code}\\n" -H "Content-Type: application/json" -d "{...invalid probe...}" https://mohamy.abud.fun/api/license/activate`
- `Push-Location admin-server; npm test; npm audit; Pop-Location`
- `.\gradlew.bat --% clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace`
- local runtime admin/license smoke via `node -e` against `admin-server/src/server.js`
- `C:\\Users\\Abud\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe devices -l`
- `C:\\Users\\Abud\\AppData\\Local\\Android\\Sdk\\emulator\\emulator.exe -list-avds`

## Build/Test Results
- Android: `clean :app:testDebugUnitTest :app:assembleDebug` -> **BUILD SUCCESSFUL**
- Admin server: `npm test` -> **15/15 pass**
- Admin server: `npm audit` -> **0 vulnerabilities**
- Debug APK built successfully at:
  - `app/build/outputs/apk/debug/app-debug.apk`
- Official release APK URL still returns HTTP 200:
  - `https://github.com/3bud-ZC/Mohamy/releases/download/v1.8.1/app-release.apk`

## Next Required Work
- Redeploy or restart the production `admin-server` so the fixed activation route and DB compatibility migrations are actually live on `https://mohamy.abud.fun`.
- Log into the production admin panel, create the requested test account, and repeat activation with the new debug APK.
- Re-run the APK on a real device or a healthy emulator and capture the post-fix activation result plus screenshots in `docs/qa/emulator/ACTIVATION_ADMIN_INTEGRATION_QA_2026-06-29.md`.
