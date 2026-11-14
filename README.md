# Habitly — Smart Habit Tracker & Personal Growth App

Habitly is a modern, offline-first native Android Habit Tracker & Personal Growth application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, **DataStore Preferences**, **WorkManager**, **Jetpack Glance**, **Firebase Authentication**, and **Cloud Firestore**.

---

## Architecture & Tech Stack

- **UI & Design**: 100% Jetpack Compose with Material 3 (`Light` & `Dark` themes, adaptive `NavigationBar` / `NavigationRail` responsive layout via `BoxWithConstraints`, custom typography with bundled `Plus Jakarta Sans`, `Inter`, and `JetBrains Mono` fonts).
- **Architecture**: MVVM + Unidirectional Data Flow (`StateFlow` + `collectAsStateWithLifecycle`) with clean domain engines (`HabitScheduleEngine`, `AchievementEngine`, `QuotesProvider`, `DataExportHelper`).
- **Navigation**: Type-safe **Navigation Compose** (`@Serializable` route keys) across all 18 screens.
- **Local Persistence (Offline-First Source of Truth)**: **Room Database** (`HabitlyDatabase`) storing `UserEntity`, `CategoryEntity`, `HabitEntity`, `HabitCompletionEntity` (unique constraint on `(habitId, dateIso)` to prevent duplicate check-ins), `DailyJournalEntity`, `AchievementEntity`, `UserAchievementEntity`, `UserSettingsEntity`, plus future-ready `ChallengeEntity` & `FriendshipEntity`.
- **Cloud Sync & Auth**: **Firebase Authentication** (Google Sign-In via Jetpack **Credential Manager**) and **Cloud Firestore** (`CloudSyncRepository`) using named database binding, server timestamps, and zero-trust owner-scoped `firestore.rules`.
- **Background Reminders & Notifications**: **WorkManager** (`ReminderWorker` + `HabitBootReceiver`) scheduling per-habit and daily summary notifications with `Done` and `Snooze 15m` notification action buttons.
- **Preferences**: **DataStore Preferences** (`AppPreferencesDataStore`) for theme mode, onboarding completion, accent preference, and last sync timestamp.
- **Home Screen Widget**: **Jetpack Glance** (`HabitlyGlanceWidget`) displaying today's habit completion progress directly on the launcher.

---

## Implemented Screens

1. **Splash Screen** — Branded entry & auth/onboarding routing
2. **Onboarding Carousel** — 3-step interactive walkthrough with generated artwork
3. **Login / Register / Forgot Password** — Google Sign-In via Credential Manager + account recovery & Firebase Console Email/Password guidance
4. **Home Dashboard** — Personalized greeting, daily motivational quote, animated progress ring, streak summary, one-tap check-in cards, upcoming reminders, and weekly overview
5. **Habit List** — Category filtering, search, active/paused/archived tabs, quick toggles
6. **Add Habit / Edit Habit** — Icon & color pickers, frequency modes (`Daily`, `Times per Week`, `Specific Weekdays`, `Monthly Dates`), start date, duration goal, reminder time, and notes
7. **Habit Details** — Streak stats (`Current Streak`, `Longest Streak`, `Completion Rate`, `Weekly/Monthly Success Rate`), edit/pause/archive/delete actions, and recent check-in history
8. **Calendar & Heatmap** — Interactive monthly calendar (completed, partial, missed, future scheduled states), date detail breakdown, and GitHub-style long-term consistency heatmap
9. **Statistics & Analytics** — Timeframe filters (`Week`, `Month`, `Year`, `All Time`), custom Compose canvas bar/line trend charts, category breakdown, and best-performing day insights
10. **Daily Journal** — Daily reflection prompts, mood selector, and reflection history
11. **Achievements & Gamification** — Points, progressive leveling system, and 9 unlockable milestone badges with celebration modal
12. **Profile / App Settings / Notification Settings / Backup & Sync** — Theme switching (`System`/`Light`/`Dark`), notification configuration, cloud sync status & manual sync trigger, and JSON export via `FileProvider`

---

## Firebase & Authentication Setup

- **Google Sign-In (Active)**: Configured via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and Firebase Auth using `app/google-services.json`.
- **Cloud Firestore Rules**: Scoped strictly per authenticated user (`/users/{userId}/...`) with field validation and immutable ownership (`userId`, `createdAt`).
- **Email/Password Authentication**: The Login, Register, and Forgot Password screens provide UI guidance. To activate Email/Password login in production, enable the **Email/Password** sign-in provider in your Firebase Console under **Authentication → Sign-in method**.

---

## Android Permissions

Declared in `app/src/main/AndroidManifest.xml` following least-privilege Google Play guidelines:
- `android.permission.INTERNET` & `ACCESS_NETWORK_STATE`: Cloud Firestore synchronization and Google Sign-In.
- `android.permission.POST_NOTIFICATIONS` (runtime requested on Android 13+): Habit reminders and daily summary notifications.
- `android.permission.RECEIVE_BOOT_COMPLETED`: Restores scheduled WorkManager habit reminders after device reboot.

---

## Testing Instructions

### 1. Firestore Security Rules Unit Tests (Node.js + Firebase Emulator)
```bash
firebase emulators:exec --only firestore,auth --project demo-no-project "FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 node --test firestore.test.js"
```

### 2. Android Unit, Robolectric & Firestore Integration Tests
Start the Firestore & Auth emulators (`firestore` on `127.0.0.1:8085`, `auth` on `127.0.0.1:9099`), then run:
```bash
gradle :app:testDebugUnitTest
```
Or run pure JVM unit tests without emulators:
```bash
gradle :app:testDebugUnitTest --tests 'com.example.domain.HabitScheduleEngineTest' --tests 'com.example.ExampleRobolectricTest'
```
