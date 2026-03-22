# usage-history

Native Android app that reads your device's app-usage events and shows them as a daily timeline.

## MVP

- Request **Usage Access** from Android settings
- Read foreground app events with `UsageStatsManager`
- Build app sessions for a selected day
- Cache the sessions in **Room**
- Render a timeline with app icon, app name, start time, and duration

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Room
- Coroutines

## Project setup

1. Open the project in Android Studio.
2. Make sure your Android SDK is installed and `local.properties` points to it.
3. Sync Gradle.
4. Run the `app` configuration on a physical Android device.

## Device setup

1. Launch the app.
2. Tap **Grant access**.
3. In Android settings, enable **Usage Access** for `Usage History`.
4. Return to the app and tap **Refresh** if needed.

## Notes and limitations

- The app uses Android's **Usage Access** API, not Accessibility.
- Timeline accuracy depends on the events Android exposes for your device and OS version.
- Very short or overlapping app transitions can be normalized when sessions are reconstructed.
- This MVP is **on-device only** and does not sync to a backend.
