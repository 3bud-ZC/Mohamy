# ACTIVATION / ADMIN INTEGRATION QA - 2026-06-29

## Scope
- Repository: `C:\Users\Abud\Desktop\GitHub\MohamyPhone`
- Goal: verify Android activation contract, deployed API routing, admin/license linkage, contrast adjustments, and APK output without touching signing assets, `applicationId`, or `update/latest.json`.

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

## Admin/API Route Checks
- `GET /` -> `200 OK`
- `GET /api/health` -> `200 OK`
- `GET /health` -> fail (`Cannot GET /health`)
- `GET /api` -> fail (`Cannot GET /api`)
- `GET /admin` -> fail (`Cannot GET /admin`)

## Production Access Check
- SSH alias found locally:
  - `vps-default` -> `root@161.35.54.6`
- Result of direct SSH auth check on `2026-06-29`:
  - `Permission denied (publickey)`
- Consequence:
  - production app path could not be inspected from this environment
  - PM2 process name/status could not be confirmed live from this environment
  - production Nginx site file could not be inspected live from this environment
  - production `.env` location could not be confirmed live from this environment
  - production SQLite DB path could not be confirmed live from this environment
- Conclusion:
  - production redeploy/restart was **not possible from this session** because the VPS does not accept the configured local key for `root`

## Production Activation Probe
- Probe route: `POST https://mohamy.abud.fun/api/license/activate`
- Payload type: harmless invalid credentials only, no destructive action
- Result: `HTTP 500`
- Conclusion: production activation failure is confirmed server-side on the deployed backend as of `2026-06-29`; Android cannot be expected to activate successfully against production until that backend is redeployed or repaired.

## Local Runtime Integration Check
- Result: **PASS**
- Flow executed locally against `admin-server/src/server.js` with a temporary SQLite DB:
  - admin login -> pass
  - create lawyer -> pass
  - activate by username -> pass
  - activate by phone -> pass
- Conclusion: the repo code now supports the required admin -> lawyer -> license -> device activation flow and both admin creation plus mobile activation use the same SQLite-backed data path (`lawyers`, `licenses`, `devices`).

## UI / Contrast Changes Verified In Code
- Activation field label corrected from an activation-code framing to password-based login wording.
- Username field clarified to `اسم المستخدم أو رقم الهاتف`.
- Input borders, notes, hint chips, secondary text, and snackbar colors were darkened/strengthened to improve contrast while preserving the premium gold/black identity.

## Build Validation
- `npm test` -> pass (`15/15`)
- `npm audit` -> pass (`0 vulnerabilities`)
- `.\gradlew.bat --% clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace` -> **BUILD SUCCESSFUL**
- Debug APK path:
  - `C:\Users\Abud\Desktop\GitHub\MohamyPhone\app\build\outputs\apk\debug\app-debug.apk`

## APK Install Commands For Manual Test
- Normal reinstall:
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- If an old conflicting install exists:
  - `adb uninstall com.aistudio.mohamyphone.lylawar`
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Emulator / Device Result
- The newer UI polish run already confirmed a healthy emulator install path and activation-screen capture from the rebuilt debug APK.
- A full live activation replay still could not be executed in this pass because the production backend remains broken and no live account could be created from this environment.

## Screenshots
- Activation-screen UI evidence exists from the rebuilt debug APK in:
  - `docs/qa/emulator/ui-polish-2026-06-29-final.png`
  - `docs/qa/emulator/ui-polish-2026-06-29-final.xml`
- Live post-login activation evidence is still pending.

## Pass / Fail Conclusion
- Repo code fix: **PASS**
- Local admin/license integration: **PASS**
- Android build + APK generation: **PASS**
- Live production activation on deployed backend: **FAIL / PENDING**
- Production redeploy/restart from this environment: **BLOCKED**
- Reason:
  - deployed `POST /api/license/activate` still returns `HTTP 500`
  - the current machine does not have an accepted VPS SSH credential
  - no production admin session was available here to create the requested live test account

## Remaining Work
- Gain working VPS access for `mohamy.abud.fun` or redeploy the fixed `admin-server` externally.
- Verify the production PM2 process and restart the correct app after deployment.
- Confirm the invalid activation probe no longer returns `HTTP 500`.
- Create the live test account:
  - name: `Test Lawyer`
  - username: `test1`
  - phone: `01000000000`
  - password: `123456`
- Install the debug APK and repeat activation on a real device or healthy emulator.
- Capture the final success/failure screenshot set here after that retest.
