# MohamyPhone Emulator QA - 2026-06-28

## Environment
- Host: Windows local machine
- AVD: `Medium_Phone`
- ADB device: `emulator-5554`
- Model: `sdk_gphone16k_x86_64`
- Android release: `17`
- App package: `com.aistudio.mohamyphone.lylawar`
- APK under test: `app/build/outputs/apk/debug/app-debug.apk`

## Automated Validation Before Manual QA
- `.\gradlew.bat --% -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest --stacktrace`
  - `BUILD SUCCESSFUL`
- `.\gradlew.bat --% -Dkotlin.compiler.execution.strategy=in-process :app:assembleDebug`
  - `BUILD SUCCESSFUL`
- `admin-server\\npm test`
  - `9/9` passed
- `admin-server\\npm audit`
  - `0 vulnerabilities`

## Initial Runtime Failure Found
- First emulator launch did not open the app UI even though install and launcher intent succeeded.
- Confirmed runtime crash from emulator logs:
  - `java.lang.NoClassDefFoundError: Failed resolution of: Lcom/example/data/AppNotificationManager;`
  - call site: `MainActivity.onCreate(MainActivity.kt:19)`
- Root cause observed in build artifacts during QA:
  - `AppNotificationManager.class` existed in Kotlin compile output
  - the corresponding dex entry was missing from the first packaged debug build
- Recovery used in this pass:
  - `.\gradlew.bat --% clean :app:assembleDebug`
  - reinstall debug APK
- Result after clean rebuild:
  - app installed successfully
  - app launched successfully on emulator
  - `topResumedActivity` returned to `com.aistudio.mohamyphone.lylawar/com.example.MainActivity`

## Manual QA Results
- App launch:
  - passed after clean rebuild and reinstall
  - splash appeared and app entered the main UI
- Activation / permission behavior:
  - Android notification permission prompt appeared on fresh reinstall
  - denying permission returned control to the app without crash
  - in-app fallback snackbar appeared correctly
- Dashboard / onboarding:
  - no auto-seed happened on startup
  - welcome card and explicit demo workspace entry were present
  - demo workspace creation worked only after explicit user action
- Demo workspace:
  - sample data appeared clearly as fake/demo content
  - verified presence of clients, cases, sessions, tasks, files, and fee-related seeded data
- Navigation:
  - dashboard, cases, clients, sessions, tasks, files library, and settings all rendered
- Cases:
  - cases list rendered correctly
  - case details opened
  - export/share path triggered Android share UI without crash
- Clients:
  - list rendered correctly
  - search/filter surface visible
- Sessions:
  - list rendered correctly
  - stats and session cards displayed clearly
- Tasks:
  - `فتح المهام` navigation worked
  - open/completed filters worked
  - task completion toggle worked
  - emulator QA changed one seeded task from open to completed in emulator-only test data
- Files library:
  - library rendered with `3 نتيجة`
  - type chips and search surface appeared
  - tapping a file opened the Android `Open with` chooser
  - share action opened the Android share chooser
  - no crash during file open/share flows
- Settings:
  - screen opened and major account/update/data sections rendered

## UI Issue Fixed In This Pass
- Issue:
  - shared top bar title/subtitle were clipped under the status bar across screens such as `الرئيسية` and `المهام`
- Fix:
  - added `statusBarsPadding()` in `app/src/main/java/com/example/ui/components/MohamyTopBar.kt`
- Verification:
  - post-fix dumps show header text moved from the unsafe top edge into a safe inset area
  - dashboard title moved from `y=37..100` to `y=100..163`
  - tasks title moved from the clipped header position to the same safe inset pattern

## Remaining Presentation Notes
- Android system permission prompt mixes English system text with the Arabic app name:
  - `Allow محامي فون to send you notifications?`
  - this is platform-owned UI, not app-owned copy
- Snackbar placement can temporarily cover lower dashboard content until dismissed
- Release/update/signing flow was intentionally not changed
- `update/latest.json` remained unchanged at `1.7.1 / code 11`

## Evidence Files
- Dashboard / onboarding:
  - `docs/qa/emulator/dashboard.png`
  - `docs/qa/emulator/after-demo-seed.png`
  - `docs/qa/emulator/dashboard-post-topbar-fix-final.png`
- Cases / clients / sessions:
  - `docs/qa/emulator/cases.png`
  - `docs/qa/emulator/case-details-clean.png`
  - `docs/qa/emulator/clients.png`
  - `docs/qa/emulator/sessions.png`
- Tasks / files / settings:
  - `docs/qa/emulator/tasks-after-direct-tap.png`
  - `docs/qa/emulator/tasks-toggled.png`
  - `docs/qa/emulator/files-library.png`
  - `docs/qa/emulator/file-open-attempt.png`
  - `docs/qa/emulator/file-share-attempt.png`
  - `docs/qa/emulator/settings.png`
- XML dumps:
  - `docs/qa/emulator/*_dump.xml`
  - `docs/qa/emulator/dashboard_post_topbar_fix_final.xml`
  - `docs/qa/emulator/tasks_post_topbar_fix.xml`
  - `docs/qa/emulator/file_open_attempt.xml`
  - `docs/qa/emulator/file_share_attempt.xml`
