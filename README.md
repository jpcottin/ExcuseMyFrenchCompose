# Excuse My French - Android Compose App

[![CI](https://github.com/jpcottin/ExcuseMyFrenchCompose/actions/workflows/ci.yml/badge.svg)](https://github.com/jpcottin/ExcuseMyFrenchCompose/actions/workflows/ci.yml)

This is a simple Android application built with Jetpack Compose that displays French insults and accompanying images. It fetches data from a public API.

## Overview

The app consists of a single screen that displays:

*   A randomly selected French insult (text).
*   An associated image.
*   A divider between the text and image.
*   A loading indicator while data is being fetched.
*   An error message if data fetching fails.
*   A Text-To-Speech (TTS) feature that reads the insult aloud in French. TTS problems are shown as a small note without hiding the insult.
*   A mute/unmute toggle button.
*   Pause/resume and next buttons to control the automatic insult refresh.
*   A level selector (1/2/3) to choose the maximum insult level: 1 = family-friendly, 2 = adds vulgar, 3 = adds offensive. Levels are cumulative, the choice is persisted across launches, and the app defaults to level 1 on first launch.
*   An AppFunction (`getFrenchInsult`) that lets on-device AI agents and the Android system fetch an insult without opening the app UI (Android 16+).
*   An adaptive launcher icon (beret-wearing robot on the French flag) with a monochrome layer for Android 13+ themed home screens.

The layout adapts to the window size. In windows narrower than 600dp (phones in portrait), the text is displayed above the image with a horizontal divider, and the text area takes up at least 15% of the window height. In wider windows (tablets, unfolded foldables, split-screen, and desktop windows), the text and image are shown side by side with a vertical divider. In both layouts the text is centered and the image keeps its aspect ratio while fitting within 90% of the available space.

## Technologies Used

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Networking:** OkHttp
*   **JSON Parsing:** kotlinx.serialization
*   **Asynchronous Programming:** Kotlin Coroutines (with `StateFlow`)
*   **Dependency Injection:** Manual dependency injection (constructor injection for Repository and `TtsService`)
*   **ViewModel:** `AndroidViewModel` (for access to application context)
*   **Text-To-Speech:** Android `TextToSpeech` API abstracted behind a `TtsService` interface.
*   **Preferences:** Jetpack DataStore (persists the mute state and insult level).
*   **Agent Integration:** Jetpack AppFunctions (`androidx.appfunctions`, processed with KSP).
*   **Testing:**
    *   Unit Tests: JUnit 4, MockK, Robolectric, `kotlinx-coroutines-test`
    *   UI Tests: `androidx.compose.ui:ui-test-junit4`
    *   Screenshot Tests: Compose Preview Screenshot Testing (reference images for phone, foldable, tablet, and desktop; run `./gradlew validateDebugScreenshotTest` to check, `updateDebugScreenshotTest` to re-record)
*   **Image Loading:** `painterResource` (for placeholder), and manual Base64 decoding and Bitmap conversion for fetched images.

## Continuous Integration

Every push and pull request to `main` runs seven GitHub Actions jobs: unit tests, screenshot validation against the committed reference images, a minified release build (guarding the R8 configuration), and instrumented tests on an emulator matrix — API 34 and 36 (blocking) plus the API 37.0 and 37.1 16 KB page-size preview images (non-blocking). Dependabot keeps GitHub Actions and Gradle dependencies up to date with weekly PRs.

## AppFunctions

On Android 16+, the app registers the `getFrenchInsult` AppFunction so on-device agents (assistants, voice commands, system shortcuts) can fetch an insult without opening the UI. It takes an optional `maxLevel` parameter (1–3, same semantics as the in-app level selector; omitted or 0 means level 1) and returns the insult text and its level. Test it from a shell:

```bash
adb shell cmd app_function execute-app-function \
  --package io.github.jpcottin.excusemyfrench \
  --function "io.github.jpcottin.excusemyfrench.appfunctions.InsultFunctions#getFrenchInsult" \
  --parameters "'{\"maxLevel\": 2}'" --brief-yaml
```

## API

The application fetches data from the following public API endpoint:

*   **Insult and Image Data:** `https://excusemyfrench.herokuapp.com/api/v1/img`

The optional `level` query parameter (`?level=1|2|3`, default `3`) caps the insult level; levels are cumulative, so `level=2` serves levels 1 and 2. The app always sends the user's selected level.

More information about the API, including its source code, can be found on GitHub: [https://github.com/jpcottin/excusemyfrench/blob/master/README.md](https://github.com/jpcottin/excusemyfrench/blob/master/README.md). The API returns a JSON response in the following format:

```json
{
  "insult": {
    "text": "French insult text here",
    "index": 123,
    "level": 2
  },
  "image": {
    "data": "/9j/4AAQSkZJRgABAQAAAQABAAD/...",
    "mimetype": "image/jpeg",
    "indexImg": 456
  }
}
```

