# Secure Bio — Attendance App

Biometric (fingerprint/face) login + QR/barcode scanning + attendance dashboard,
with fast local storage and Firebase cloud backup.

## Why this is faster than a typical version
- **Room + WAL journal mode**: local writes happen instantly on scan, before any
  network call — attendance is never blocked waiting on the internet.
- **CameraX `STRATEGY_KEEP_ONLY_LATEST`**: the scanner always analyzes the newest
  camera frame instead of queuing up stale ones, so scanning feels instant.
- **Background cloud sync**: Firebase upload happens after the local save, on a
  coroutine, so the UI never freezes waiting for the network.
- **Flow-based live dashboard**: the chart updates automatically when a new
  record is inserted — no manual refresh/re-query.

## Setup (Android Studio)
1. Open this folder in Android Studio (Hedgehog or newer).
2. Create a Firebase project at https://console.firebase.google.com, add an
   Android app with package name `com.securebio.attendance`, and download
   `google-services.json` into the `app/` folder.
3. Enable **Firestore Database** in the Firebase console (start in test mode
   for development).
4. Let Gradle sync — it will download all dependencies (needs internet).
5. Run on a device/emulator with a fingerprint or face unlock configured.

## Project structure
```
app/src/main/java/com/securebio/attendance/
  ui/          LoginActivity, ScanActivity, DashboardActivity
  data/        Room entity/DAO/database, FirebaseRepository
  utils/       CsvExporter
```

## Notes
- `ScanActivity` currently uses the scanned code as both ID and name — wire up
  `importRoster()` in `CsvExporter` to look up real names from an uploaded
  roster CSV.
- Minimum SDK 26 (Android 8.0) — required for AES-backed biometric APIs.
