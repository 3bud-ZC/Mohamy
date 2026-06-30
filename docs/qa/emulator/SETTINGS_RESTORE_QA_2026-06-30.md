# QA Report: Settings Restoration / 2026-06-30

## Objective
Restore and reorganize `SettingsScreen` into a professional, commercial-grade settings screen without hiding important user-facing actions behind Advanced mode.

## Old Issue
- The previous polish simplified Settings too aggressively.
- Critical actions like **backup**, **restore**, **file recovery**, **rights / حقوقي**, **about**, and **privacy** were either hidden or not clearly exposed.
- The screen felt incomplete for a commercial product.

## What Was Restored

### 1. Professional section architecture
| Section | Contents | Always visible |
|---|---|---|
| `الحساب والمكتب` | Lawyer name, office name, office phone, bar number, save locally | Yes |
| `الترخيص والتفعيل` | License status, account code, logout | Yes |
| `الإشعارات` | Phone notification permission, session/task reminders, daily summary | Yes |
| `البيانات المحلية` | Create backup, restore backup, file recovery, data export, open files library | Yes |
| `المظهر والمساعد` | Dark mode, cloud assistant toggle, smart assistant shortcut | Yes |
| `الحقوق والخصوصية` | Rights dialog, privacy/about dialog, support link, privacy bullets | Yes |
| `أدوات متقدمة` | Demo data, welcome card, technical updates | Advanced mode only |

### 2. Rights / About / Privacy
- Added `RightsDialog` with Arabic terms-of-use copy.
- Added `AboutDialog` showing app name, version from `BuildConfig`, privacy summary, and local-first note.
- Added privacy bullets explaining that case files are not uploaded to the server.

### 3. Backup / Restore / File Recovery
- `البيانات المحلية` section is always visible.
- `إنشاء نسخة احتياطية` navigates to the existing `BackupRestoreScreen`.
- `استعادة نسخة احتياطية` shows a confirmation dialog before navigating to the restore screen.
- `استرداد الملفات` opens `FilesLibrary` as a safe placeholder (full file recovery still planned).
- `تصدير البيانات` keeps the existing `ImportData` navigation.
- `فتح مكتبة المستندات` opens the files library directly.

### 4. Confirmation dialogs
- Logout: warns the user that current workspace will be saved locally, then returns to activation.
- Restore backup: warns that current data will be replaced.

### 5. Advanced mode preserved
- Toggle is still available.
- Only technical/demo tools (demo data, welcome card, server URL, technical updates) are behind it.

## Files Changed
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- `STATUS.md`
- `docs/qa/emulator/SETTINGS_RESTORE_QA_2026-06-30.md` (this file)

## Screenshots
- No screenshots available; manual device/emulator QA still pending.

## Pass / Fail Notes
| Check | Result | Notes |
|---|---|---|
| Android `:app:compileDebugKotlin` | Pass | SettingsScreen reorganized and compiles |
| Android `:app:testDebugUnitTest` | Pass |  |
| Android `:app:assembleDebug` | Pass | `app-debug.apk` produced |
| Android `:app:assembleRelease` | Pass | `app-release.apk` produced |
| Admin `npm test` | Pass | 15/15 |
| Admin `npm audit` | Pass | 0 vulnerabilities |
| UI: all sections visible by default | Pass | Advanced mode only hides technical tools |
| UI: rights/about/privacy dialogs | Pass | Dialogs implemented and wired |
| UI: backup/restore/file recovery visible | Pass | Under `البيانات المحلية` |
| UI: confirmation dialogs for dangerous actions | Pass | Logout and restore backup confirmed |
| Manual emulator smoke | Pending | adb not available in this environment |

## Remaining Settings Tasks
- Capture screenshots on a real device/BlueStacks in light and dark modes.
- Verify backup actually creates a `.mpb` file and restore flow works end-to-end.
- Verify file recovery / files library navigation opens correctly.
- Consider a separate toggle for daily summary vs. session/task reminders (currently shared).
- Consider adding font-size setting if/when the typography system supports it.
