---
description: Lawyer-facing presentation QA on emulator
---

## Environment
- Emulator: Medium_Phone (emulator-5554)
- Device model: sdk_gphone16k_x86_64
- Android release: 17
- APK: app/build/outputs/apk/debug/app-debug.apk

## Install & Launch
- Install: success (`adb install -r app-debug.apk`)
- Launch: success (`adb shell monkey -p com.aistudio.mohamyphone.lylawar -c android.intent.category.LAUNCHER 1`)

## Flows Tested
- Splash → Activation/Login → Dashboard welcome card
- Demo workspace creation (explicit action only)
- Dashboard stats and quick actions
- Cases list & case details (share/export intent)
- Clients list & search surface visibility
- Sessions list & card readability
- Tasks list, open/completed filters, completion toggle
- Files library (open chooser + share chooser)
- Settings (demo seed entry, backup/update sections)

## Copy/UI Adjustments in this pass
- Dashboard greeting defaults to a professional Arabic salutation when the lawyer name is blank or numeric and keeps the name with a polite prefix otherwise.
- Global snackbars now respect navigation bar padding with added bottom spacing to avoid overlapping lower dashboard content.

## Observations
- Arabic layout renders correctly; no clipped top bars (carried from prior fix).
- Demo data appears only after explicit trigger; sample entries look obviously fake.
- Snackbars respect nav insets and sit clear of lower content after padding fix.
- No crashes during navigation, file open/share, or task/session/case interactions.

## Issues Found
- None blocking after snackbar padding fix.

## Recommendation
- Ready for lawyer-facing emulator demo; release/signing work remains pending.

## Commands
- `adb devices`
- `adb shell getprop ro.product.model`
- `adb shell getprop ro.build.version.release`
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb shell monkey -p com.aistudio.mohamyphone.lylawar -c android.intent.category.LAUNCHER 1`
