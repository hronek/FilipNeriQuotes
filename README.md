# Filip Neri Quotes

An Android app that shows a "quote of the day" from St. Philip Neri, with an optional daily notification at a user-selected time. The app supports multiple UI and quotes languages and uses public-domain background images.

## Features
- Show the quote of the day on launch.
- Schedule a daily local notification at a chosen time.
- UI and quotes localization: cs, pl, it, de, es, fr, en (fallback: en).
- Quotes stored per language in assets as text files, one quote per line.
- Public-domain images used as backgrounds.
- Brochure available from the app menu.

## Requirements
- Android Studio (current version) with Android SDK.
- JDK (bundled with Android Studio is fine).
- Gradle wrapper included in the project.

## Getting started (Development)
1. Open the project in Android Studio (File → Open… and select the project folder).
2. Let Android Studio sync dependencies and finish indexing.
3. Run on an emulator or a connected device (Run ▶).

## Build
- Debug: select the `debug` build variant and run.
- Release: configure signing in Android Studio and create a signed APK/AAB.

## Localization
- UI strings are under `app/src/main/res/values-*/strings.xml` (e.g., `values-cs`, `values-pl`, ...).
- Quotes are stored per language under `app/src/main/assets/<lang>/...`.
- English is the fallback if a language is missing.

## Quotes format in assets
- Each language file contains one quote per line.
- Line format: `MM/D. Quote text`
  - Example: `01/1. Be cheerful...`
- The app selects the quote matching the current date.

## Brochure
- The brochure is available from the app menu.
- Source files are under `app/src/main/assets` (Windows path: `\\FilipNeriQuotes\\app\\src\\main\\assets`).

## Notifications
- Users select a daily reminder time in-app.
- The app schedules a local notification at that time every day.
- Ensure notifications are allowed in Android settings.

## Not versioned (.gitignore)
- Build artifacts (`build/`, `.gradle/`, etc.).
- Local configuration (`local.properties`).
- IDE files (`.idea/`, `*.iml`), logs, OS temp files.
- Private keys and keystore files.

## Licenses for images and texts
- Background images are from the public domain.
- Quotes and the brochure are sourced from https://www.oratoriosanfilippo.org/.
- Ensure any new content added has clear licensing.
