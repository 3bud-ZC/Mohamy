# MohamyPhone UI Redesign Plan

## Phase
- UI/UX Rebuild Phase 2C complete

## Reference Direction
- Premium dark legal office interface
- Arabic-first layout
- Full RTL presentation
- Native Android Jetpack Compose implementation only
- HTML/Tailwind reference files are visual inspiration only and are not used at runtime

## Visual Tone
- Serious and premium
- Trustworthy and private
- Luxury law office atmosphere
- Egyptian / Arabic legal workflow context
- Minimal legal symbolism with scales, courthouse geometry, file stacks, calendar markers, and seal-like accents

## Core Color Direction
- Background: `#121212` / `#131313`
- Surface/Card: `#1E1E1E` / `#202020`
- Gold Accent: `#D4AF37` / `#E9C349`
- Warm Text: `#F5F5DC` / `#E5E2E1`
- Muted Text: `#C4C7C7`
- Danger: deep ruby
- Success: deep emerald

## Typography Direction
- Arabic-first
- RTL by default
- IBM Plex Sans Arabic is the visual reference direction
- Existing Android implementation uses native Compose typography with Arabic-friendly font resources already in the app

## Completed Scope
- Phase 1:
  - reusable premium Compose design system
  - splash screen refresh
  - login / activation screen refresh
  - dashboard refresh
  - app shell top bar / bottom navigation refresh
- Phase 2A:
  - cases list refresh
  - case details structure refresh
  - clients list refresh
- Phase 2B:
  - sessions list refresh
  - tasks list refresh
  - files / documents library refresh
  - settings screen regrouped into premium sections
- Phase 2C:
  - commercial demo mode
  - onboarding / welcome flow polish
  - empty workspace polish across core screens
  - safe local demo seeding controls

## Components Added
- `MohamyCard`
- `MohamyButton`
- `MohamyTopBar`
- `MohamyBottomNav`
- `MohamyStatusBadge`
- `MohamySearchBar`
- `MohamyEmptyState`
- `CaseCard`
- `ClientCard`
- `CaseInfoSection`
- `CaseTimelineItem`
- `QuickActionTile`
- `SessionCard`
- `TaskCard`
- `FileDocumentCard`
- `SettingsSection`
- `SettingsActionRow`
- `MohamyEmptyState` secondary action support for onboarding/demo entry points

## Refreshed Screens
- `SplashScreen.kt`
- `ActivationScreen.kt`
- `DashboardScreen.kt`
- `CasesScreen.kt`
- `ClientsScreen.kt`
- `SessionsScreen.kt`
- `TasksScreen.kt`
- `FilesLibraryScreen.kt`
- `SettingsScreen.kt`

## Demo / Onboarding Strategy
- Chosen approach: explicit local database seed only on user action
- Demo data is never created automatically on first run or activation
- Demo seed is per local workspace/account and stays fully offline-first
- Demo data is clearly fake Arabic sample data for lawyers only
- Existing user data is never overwritten
- If the workspace already contains data, the app shows a confirmation dialog before appending the sample records
- Onboarding persistence is local-only and lightweight through workspace-scoped preferences

## Seeded Demo Scope
- `3` sample clients
- `5` sample cases
- `4` sample sessions
- `5` sample tasks
- `3` local text file records backed by valid private-storage `.txt` files
- `2` fee records

## Screens Affected By Phase 2C
- `DashboardScreen.kt`
  - welcome/value card
  - start using app action
  - create/open demo workspace action
  - settings/profile shortcut
  - safer empty-state demo entry points
- `SettingsScreen.kt`
  - demo seed action under backup/data
  - non-destructive warning copy
  - reopen welcome card action
- `CasesScreen.kt`
  - demo CTA in the empty state when the workspace is blank
- `ClientsScreen.kt`
  - demo CTA in the empty state
- `SessionsScreen.kt`
  - next-step guidance when no sessions exist
  - demo CTA in the empty state
- `TasksScreen.kt`
  - demo CTA in the empty state
- `FilesLibraryScreen.kt`
  - demo CTA in the empty state
- `data/AppViewModel.kt`
  - workspace presentation state, onboarding actions, demo seed command
- `data/Repository.kt`
  - workspace-scoped onboarding/demo flags
  - safe sample data seeding

## First-Run Behavior Notes
- There was no existing onboarding persistence before this pass
- The repository already had local account workspace switching/snapshot support, which made local demo seeding safe enough
- Template seeding already existed on startup, but demo clients/cases/sessions/tasks/files did not
- The app now prefers a non-blocking welcome card instead of a forced onboarding wall

## Manual QA Checklist For Lawyer Presentation
- Activate or open an account and confirm the dashboard welcome card appears only for that local workspace
- Press `ابدأ استخدام التطبيق` and confirm the card hides and navigation continues normally
- Reopen the welcome card from settings and confirm it returns without affecting legal data
- Create demo workspace from an empty account and verify sample clients, cases, sessions, tasks, files, and fees appear
- Trigger demo creation from a workspace that already has real data and confirm the warning appears before append
- Open the seeded cases/files and confirm the sample content is clearly fake and stored locally
- Check empty states on dashboard, cases, clients, sessions, tasks, and files for clear next-step actions
- Confirm logout/login preserves each account workspace independently

## Remaining Commercial Polish
- Real-device presentation pass for update/install flow
- Final lawyer-facing copy polish after hands-on demo feedback
- Release packaging/signing/publish work remains intentionally outside this redesign plan

## Validation Snapshot
- Completed:
  - `:app:testDebugUnitTest`
  - `:app:assembleDebug`
  - `admin-server npm test`
  - `admin-server npm audit`
- Remaining polish:
  - real-device update test
  - release packaging

## Implementation Notes
- Preserve current business logic and navigation model
- Preserve local-first / offline-first behavior
- Keep colors and shape tokens centralized
- Keep Material 3 as the base system
- Avoid heavy dependencies
- Keep release/update publishing work outside this redesign phase
- `FilesLibraryScreen.kt` is the actual files/documents library screen in the current repo
- `update/latest.json` remains intentionally unchanged
