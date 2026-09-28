# WhisperVault

Privacy-first **offline** audio transcription for Android. Select audio on your phone, transcribe fully on-device with whisper.cpp, and save text locally. Audio never leaves the device.

## Privacy rules

- No uploads of audio or transcripts
- No OpenAI (or any cloud) API calls
- No in-app AI agent
- No `INTERNET` permission for transcription
- Models stay on device (user-provided ggml files)

## Status

Phase 1 — Android project scaffold with Compose home screen, package layout, and JNI CMake skeleton.

## Build prerequisites

- JDK 17+
- Android SDK Platform 35, Build-Tools 35+
- Android NDK (r27 recommended) and CMake 3.22.1
- Gradle wrapper included (`./gradlew`)

Create `local.properties` (gitignored) with your SDK path:

```properties
sdk.dir=/path/to/Android/sdk
```

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

## Module map

| Path | Role |
|------|------|
| `app/src/main/java/com/whispervault/ui/` | Compose UI (Material3) |
| `app/src/main/java/com/whispervault/viewmodel/` | MVVM state |
| `app/src/main/java/com/whispervault/data/` | Domain / persistence models |
| `app/src/main/java/com/whispervault/storage/` | SAF / file access |
| `app/src/main/java/com/whispervault/transcription/` | Offline ASR facade + JNI |
| `app/src/main/java/com/whispervault/worker/` | WorkManager / foreground work |
| `app/src/main/cpp/` | JNI + native CMake |
| `native/` | Native integration notes |
| `third_party/whisper.cpp/` | Vendored whisper.cpp (ASR phase) |

## License

MIT — see [LICENSE](LICENSE).
