# ExpenseIQ

[![Platform](https://img.shields.io/badge/Android-36-brightgreen?logo=android)](https://developer.android.com/about/versions/14)
[![Language](https://img.shields.io/badge/Kotlin-2.2.10-blue?logo=kotlin)](https://kotlinlang.org)
[![Build](https://img.shields.io/badge/Gradle-9.5.1-blue?logo=gradle)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Smart, offline-first budgeting and expense tracker for Android** featuring automated SMS UPI receipt parsing, balance dashboards, budget limits, analytics charts, and full backup/restore.

## Features

- **SMS Parsing** — Automatically detects and parses Indian banking SMS (UPI, debit/credit card transactions) using regex patterns
- **Real-time Notifications** — Incoming SMS triggers a confirmation prompt to approve/log transactions instantly
- **Dashboard** — At-a-glance view of net worth, budget health, and recent transactions
- **Transaction Book** — Search, filter, sort, and export transactions to CSV
- **Budgets** — Set monthly budget limits per category with progress bars
- **Accounts** — Manage multiple accounts (cash, savings, credit card, wallets) with transfer between accounts
- **Reports & Analytics** — Pie charts, bar charts, and PDF export; smart advisor powered by Gemini AI
- **Backup & Restore** — Full JSON backup/restore to/from device storage
- **Offline-first** — All data stored locally via Room (SQLite); no internet required for core features
- **Material You** — Dynamic color theming (Material 3)

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM (ViewModel, Repository, DAO) |
| Database | Room (SQLite) with KSP |
| Networking | Retrofit 2 + OkHttp 4 + Moshi |
| Async | Coroutines & Flow |
| Background | WorkManager |
| Images | Coil |
| AI | Firebase AI (Gemini) |
| Testing | JUnit 4, Robolectric, Roborazzi |

## Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- [JDK 21](https://adoptium.net/temurin/releases/)
- Android SDK 36 (managed via Android Studio)

## Getting Started

```bash
git clone https://github.com/mine-nandha/expenseiq.git
cd expenseiq
./gradlew assembleDebug
```

Open the project in Android Studio, connect a device or start an emulator, and run.

## Configuration

### Environment Variables
Copy `.env.example` to `.env` if you use the Secrets Gradle Plugin:
```bash
cp .env.example .env
```

### Release Signing
The release build reads the following environment variables:
- `KEYSTORE_PATH` — path to your keystore (falls back to `release.keystore`)
- `STORE_PASSWORD` — keystore password
- `KEY_ALIAS` — key alias
- `KEY_PASSWORD` — key password

Debug builds use `debug.keystore` with the well-known `android` password.

## Building & Running

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew bundleRelease          # Android App Bundle (AAB)

# Install via ADB
adb install app/build/outputs/apk/debug/*.apk
```

## Testing

```bash
./gradlew test                    # Unit tests + Robolectric
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator required)
```

## Project Structure

```
app/
├── src/main/java/com/mine/expenseiq/
│   ├── MainActivity.kt              # Entry point
│   ├── data/
│   │   ├── local/                   # Room database & DAO
│   │   ├── model/                   # Entity models
│   │   └── repository/              # Repository layer
│   ├── viewmodel/                   # ViewModels
│   ├── ui/
│   │   ├── screens/                 # Dashboard, Transactions, Budgets, Accounts, Reports, Profile
│   │   ├── components/              # SMS confirmation prompt, import dialog
│   │   └── theme/                   # Colors, Theme, Typography
│   ├── receivers/                   # SMS BroadcastReceiver
│   ├── workers/                     # WorkManager periodic sync
│   └── utils/                       # SMS parser, CSV exporter, event trigger
├── src/test/                        # Unit & Robolectric tests
└── src/androidTest/                 # Instrumented tests
```

## CI/CD

Two GitHub Actions workflows are included:

- **android-ci.yml** — Runs on PRs and pushes to `main`. Executes `./gradlew test` and `./gradlew assembleDebug`, uploading the debug APK as an artifact.
- **release.yml** — Runs on tags matching `v*`. Builds a signed release APK and AAB, then creates a GitHub Release with both artifacts attached.

## Notes

> The root project name in `settings.gradle.kts` is currently `"My Application"` (Android Studio default). Consider renaming it to `"ExpenseIQ"`:
> ```kotlin
> rootProject.name = "ExpenseIQ"
> ```

## License

[MIT](LICENSE) © 2026 Nandha Kishore
