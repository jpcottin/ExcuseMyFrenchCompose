# Excuse My French - Android Compose App

This is a simple Android application built with Jetpack Compose that displays French insults and accompanying images. It fetches data from a public API.

## Overview

The app consists of a single screen that displays:

*   A randomly selected French insult (text).
*   An associated image.
*   A horizontal divider between the text and image.
*   A loading indicator while data is being fetched.
*   An error message if data fetching fails.
*   A Text-To-Speech (TTS) feature that reads the insult aloud in French.
*   A mute/unmute toggle button.
*   A level selector (1/2/3) to choose the maximum insult level: 1 = family-friendly, 2 = adds vulgar, 3 = adds offensive. Levels are cumulative, the choice is persisted across launches, and the app defaults to level 1 on first launch.

The text is displayed prominently, taking up at least 15% of the screen height, and is centered both horizontally and vertically. The image is displayed below the divider, maintaining its aspect ratio and fitting within 90% of the available width or height.

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
*   **Testing:**
    *   Unit Tests: JUnit 4, MockK, Robolectric, `kotlinx-coroutines-test`
    *   UI Tests: `androidx.compose.ui:ui-test-junit4`
*   **Image Loading:** `painterResource` (for placeholder), and manual Base64 decoding and Bitmap conversion for fetched images.

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

