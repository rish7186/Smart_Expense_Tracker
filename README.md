# Smart Expense Tracker (Android)

The Android app is being built as a native Kotlin + Jetpack Compose application. The existing Flutter sources and non-Android platform folders remain in the repository; Android now builds from the native `android` Gradle project.

## Current milestone

The native app includes:

- Material 3 light/dark theming and bottom navigation for Home, Transactions, Analytics, Budgets, and Settings.
- Local Room storage for transactions and monthly budgets.
- Integer paise storage and validated rupee amount input (no floating-point amounts in the database).
- Manual income/expense entry, search and type filters, transaction details, exclusion, and confirmed deletion.
- Cash-flow and category summaries. Transfers are excluded; refunds reduce spending and increase cash flow; pending, failed, and excluded entries do not affect totals.
- Overall and category monthly budgets.
- On-device budget coach with actual-over-budget and month-end pace warnings, plus tailored daily spending guidance from the user's recorded transactions. No data is sent to a remote AI service.
- Optional notification-listener detection for bank transaction SMS notifications and payment alerts from Google Pay, PhonePe, and Paytm. It starts only after the user explicitly enables notification access in Android Settings; unrelated notifications and SMS inbox contents are not read.

Transaction data stays in the app's local SQLite database. The dashboard's net cash flow is calculated from recorded transactions; it is not represented as a verified bank balance.

## Build and run

1. Open this project folder in Android Studio. The root Gradle project maps the `app` module to `android/app`.
2. In Android Studio's Gradle settings, select JDK 17 or 21, then allow Gradle to sync. Kotlin 2.2.20 currently fails under JDK 25.
3. Run the `app` configuration on an Android 8.0+ device/emulator.

From PowerShell at the project root, with JDK 17 or 21 configured:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The Android-only Gradle project remains available from `android` using `.\gradlew.bat` there as well.

The minimum Android API is 26. The Android module targets API 36.

## Project structure

- `android/app/src/main/kotlin/com/example/expense_tracker/data`: Room entities, DAOs, repository, money handling, and financial calculations.
- `android/app/src/main/kotlin/com/example/expense_tracker/ui`: Compose navigation, screens, dialogs, and ViewModel.
- `android/app/src/main/AndroidManifest.xml`: declares the notification listener, which Android keeps disabled until the user grants notification access. No SMS or network permission is declared.

## Next development steps

Notification detection supports recognized transaction alerts surfaced by Google Messages, AOSP Messages, Samsung Messages, Google Pay, PhonePe, and Paytm. After notification access is enabled by the user from Settings → Automatic transaction detection, it processes active notifications when Android reconnects the listener and handles new notifications automatically. Unsupported, failed, pending, and balance-only notifications are ignored. SMS inbox access and private payment-app data are not accessed.

The budget coach evaluates completed, non-excluded transactions against monthly overall and category budgets. It warns if a target is reached/exceeded or if the current month-to-date pace projects an overrun, and offers local, rules-based suggestions including an approximate daily allowance. Its guidance is informational, not a guarantee.
