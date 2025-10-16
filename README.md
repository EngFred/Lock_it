# LockIt

**LockIt** — A privacy-first Android app locker that protects selected apps with a secure PIN and optional biometrics. Built with **Kotlin** and **Jetpack Compose**, LockIt uses Android Accessibility and Usage Access to detect foreground apps in real time and presents a robust lockscreen for protected apps. Designed for developers and privacy-conscious users, LockIt focuses on reliable protection, minimal permissions, and a polished UX. It also helps users configure call-forwarding-like protections and assists with accessibility/usage access setup flows.

---

<div align="center">
  <img src="https://github.com/user-attachments/assets/59d67d62-59c4-48d9-a0e4-4478777887ec" alt="LockIt Screenshot 1" width="40%"/>
  <img src="https://github.com/user-attachments/assets/e1f51604-ba4c-4a61-aded-756be14e471e" alt="LockIt Screenshot 2" width="40%"/>
</div>

---

## What the app does

LockIt provides real-time app locking by:

* Detecting the foreground app using Accessibility and Usage Access.
* Showing a secure lockscreen (PIN + optional biometric) when a protected app is opened.
* Letting users choose which apps to lock and configure per-app settings.
* Offering setup helpers that guide users through granting Accessibility and Usage Access permissions with clear, safe instructions.
* Minimizing permissions and keeping all lock lists and secrets local to the device for privacy.

---

## Key Features

* Secure PIN-based lock and optional biometric unlock (Android Biometric API).
* Real-time foreground app detection using Accessibility/Usage Access.
* Per-app lock settings (enable/disable, timeout, skip on trusted networks).
* Smooth Compose UI with modern UX affordances (animated lockscreen, easy onboarding flows).
* Local storage of settings and locks via Room / DataStore.
* Background workers (WorkManager) for housekeeping and optional policy updates.

---

## Project Structure

This project uses a layered architecture. Typical layout:

```
app/
├─ src/main/java/
│  ├─ data/            # Repositories, Room DB, data sources, DTOs
│  ├─ domain/          # Entities, use-cases, repository interfaces
│  ├─ presentation/    # Compose screens, ViewModels, navigation
│  ├─ service/         # Accessibility Service, usage/access listeners
│  └─ di/              # Hilt modules and bindings
└─ build.gradle
```

* **data/**: Implements repository contracts, persists lock lists and settings, normalizes data.
* **domain/**: Pure business logic (use-cases) and interfaces for testability.
* **presentation/**: Compose UI, viewmodels, and permission/onboarding flows.
* **service/**: AccessibilityService and other components that detect foreground apps and trigger the lockscreen.
* **di/**: Hilt modules to wire dependencies across app components.

---

### Libraries & Purpose

* `androidx.core:core-ktx` — Kotlin extensions for cleaner Android APIs.
* `androidx.activity:activity-compose` — Activity integration for Compose-based UIs.
* `androidx.compose` (BOM + ui/tooling) — Compose UI toolkit, tooling, and previews for modern UI.
* `androidx.lifecycle:lifecycle-runtime-ktx` — Lifecycle-aware components and coroutines support.
* `com.google.dagger:hilt-android` & `hilt-android-compiler` — Dependency injection to keep modules testable and decoupled.
* `androidx.room:room-runtime`, `room-ktx`, `room-compiler` — Local persistence for app locks, rules, and settings.
* `org.jetbrains.kotlinx:kotlinx-coroutines-android` — Concurrency primitives for background tasks and service interactions.
* `com.google.accompanist:accompanist-permissions` — Easier Compose permission flows for runtime permissions.
* `androidx.datastore:datastore-preferences` — Persist lightweight app preferences (lock timeout, themes).
* `io.coil-kt:coil-compose` — Image loading for any app icons or avatars in the UI.
* `androidx.biometric:biometric` — Optional biometric authentication for unlocking.
* `com.google.devtools.ksp` — KSP for code generation where necessary (Room, etc.).
* `androidx.core:core-splashscreen` — Modern splash screen support.

All dependencies were chosen to provide a secure, testable, and modern Compose codebase while keeping permissions and privacy front-and-center.

---

## Security & Privacy Considerations

* Lock lists, PINs, and sensitive settings are stored locally — never uploaded to remote servers without explicit user consent.
* Use Android `BiometricPrompt` for biometric unlock; fallback to secure PIN stored with encryption or using Android's Keystore where appropriate.
* Only request Accessibility and Usage Access when needed and clearly explain why the permissions are required in the onboarding flow.
* Minimize background processing and avoid collecting unnecessary telemetry.

---

## Accessibility & Permission Flows

The app includes guided flows to help users grant:

* **Accessibility Service** — required for robust foreground app detection on many Android versions.
* **Usage Access** — alternate method to detect app usage when Accessibility is not available/allowed.

Each flow shows step-by-step instructions and checks to confirm the permission was granted successfully before enabling app locking.
