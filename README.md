# SuperAI

Native Android Kotlin/Compose Superagent — floating HUD, agent profiles, on-device APK compiler, Google Drive sync.

**Latest:** May 2026

## Features

- **Floating HUD** - Always-accessible agent interface
- **Agent Profiles** - Manage multiple agent configurations
- **On-Device APK Compiler** - Compile and build directly on device
- **Google Drive Sync** - Seamless cloud synchronization
- **Jetpack Compose UI** - Modern declarative Android UI
- **Hilt Dependency Injection** - Clean architecture with DI
- **Room Database** - Local data persistence
- **Kotlin Coroutines** - Asynchronous operations

## Requirements

- Android 10+ (API 29+)
- Kotlin 2.0.21
- Gradle 8.4.0
- JDK 17

## Setup

### Prerequisites

1. Clone the repository
2. Open in Android Studio (Electric Eel or later)
3. Set up your `local.properties` file (see `local.properties.example`)

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew installDebug
```

### Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Project Structure

```
SuperAI/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                 # Gradle wrapper files
├── build.gradle.kts       # Root build configuration
├── settings.gradle.kts    # Project configuration
├── gradle.properties      # Gradle properties
└── local.properties       # Local configuration (not committed)
```

## Technologies

- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM with Hilt DI
- **Database:** Room
- **Async:** Kotlin Coroutines
- **Serialization:** kotlinx.serialization
- **Networking:** OkHttp + Google API Client
- **Cloud:** Google Drive API
- **Logging:** Timber

## Configuration

See `local.properties.example` for required local configuration settings.

## CI/CD

Automated builds run on push and pull requests via GitHub Actions. See `.github/workflows/build.yml`.

## License

TBD

## Author

krisdillman97-cyber

---

**Built with:** Kotlin | Compose | Android | Hilt | Room | Coroutines
