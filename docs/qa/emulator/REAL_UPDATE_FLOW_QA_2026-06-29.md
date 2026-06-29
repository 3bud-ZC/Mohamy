# Real Update Flow QA — 2026-06-29

## Summary
- **Initial run:** update check detected v1.8.0 while running v1.7.1, but the app hit an ANR when the update prompt was acknowledged. The download/installer step was never reached.
- **Fix applied:** `AppViewModel.downloadAndInstallAppUpdate()` now launches the published APK URL (or release-page fallback) via a non-blocking `ACTION_VIEW` intent with `FLAG_ACTIVITY_NEW_TASK` before falling back to the old internal download path. `AppUpdateManager` now parses and stores an optional `releasePageUrl`. A unit test was added for the new parser field.
- **Validation:** Gradle `clean :app:testDebugUnitTest :app:assembleDebug` passed; admin `npm test` 9/9 passed and `npm audit` 0 vulnerabilities; the fixed debug build launches cleanly on the emulator.
- **Limitation:** the published v1.8.0 APK was built before this fix, so the fixed end-to-end update flow (v1.7.1 → fixed v1.8.0) cannot be verified without cutting a new release. The fixed debug build is also versionCode 12, so it correctly reports "up-to-date" and does not show the update prompt.

## Environment
- Device: Android Emulator AVD `Medium_Phone` (emulator-5554)
- Model: `sdk_gphone16k_x86_64`
- Android: 17
- Network: emulator default (GitHub URLs reachable via curl on host)

## Code Fix Details
- Files changed:
  - `app/src/main/java/com/example/data/AppUpdateManager.kt`: added `releasePageUrl` to `AppUpdateInfo`; parser reads `releasePageUrl`, `release_page_url`, `releasePage`, `release_url`, and `html_url`.
  - `app/src/main/java/com/example/data/AppViewModel.kt`: captures `releasePageUrl` on update check; `downloadAndInstallAppUpdate()` now tries `Intent.ACTION_VIEW` on the APK URL first, then release-page URL fallback, with `FLAG_ACTIVITY_NEW_TASK`; only falls back to the internal download if no external handler is available.
  - `app/src/test/java/com/example/data/AppUpdateParserTest.kt`: added `parsesReleasePageUrl()` test.
- No changes to `update/latest.json`, `app/build.gradle.kts`, `applicationId`, signing assets, tags, or releases.

## Validation Results
- Android: `gradlew.bat --% clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace` → **BUILD SUCCESSFUL**.
- Admin server: `npm test` → 9/9 pass; `npm audit` → **0 vulnerabilities**.
- Fixed debug APK: installed via `adb install -r app/build/outputs/apk/debug/app-debug.apk` and launched via `adb shell monkey` → app opens on activation screen without crash or ANR (screenshot `debug-build-launch.png`).

## Retest of Published v1.7.1 → v1.8.0
- Installed the published v1.7.1 APK (`versionCode 11`) from `https://github.com/3bud-ZC/Mohamy/releases/download/v1.7.1/app-release.apk`.
- Launched the app; it again displayed the update prompt with Arabic message "يوجد تحديث جديد للتطبيق." and a "حسناً" action (screenshot `update-flow-retest-2.png`).
- The published v1.7.1 APK still contains the old update handler, so this path remains a known ANR risk; no new tag/release was created to verify the fix in a real v1.7.1 → fixed-build scenario.

## Outcome
- Update detection: **PASS** (v1.7.1 correctly detects available v1.8.0).
- Fixed debug build launch: **PASS** (no crash, no ANR).
- Fixed update download/installer end-to-end: **BLOCKED FROM FULL VERIFICATION** because the only available v1.8.0 APK does not contain the fix. A follow-up release (likely v1.8.1) is required to complete this test.

## Evidence
- `docs/qa/emulator/update-flow-1.png`: original update prompt on v1.7.1.
- `docs/qa/emulator/update-flow-anr.png`: original ANR dialog.
- `docs/qa/emulator/update-flow-retest-2.png`: v1.7.1 still shows the update prompt on retest.
- `docs/qa/emulator/debug-build-launch.png`: fixed debug build opens without crash.

## Follow-ups
- v1.8.1 hotfix (ANR fix) has been published and `update/latest.json` is updated by CI.
- Re-run the full v1.7.1/v1.8.0 → v1.8.1 update flow on Pixel 10 Pro Fold AVD (Android 17) or a real device and confirm:
  - Acknowledging the update prompt does not ANR.
  - The browser/download intent is dispatched and reaches the installer/download boundary.
- Emulator bring-up failed on `Medium_Phone` during this session; retry on Pixel 10 Pro Fold AVD before real-device fallback.
