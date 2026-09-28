# WhisperVault

Privacy-first **offline** audio transcription for Android. Select audio on your phone, transcribe fully on-device with whisper.cpp, and save text locally. Audio never leaves the device.

## Privacy rules

- No uploads of audio or transcripts
- No OpenAI (or any cloud) API calls
- No in-app AI agent
- No `INTERNET` permission for transcription
- Models stay on device (user-provided ggml files)

## Status

Phase 4 — Batch queue with Room + WorkManager + foreground service. Select up to 100 files, enqueue, and keep running with the screen locked. Export formats arrive next.

## ggml models (on device only)

Download official ggml weights yourself (e.g. from the [whisper.cpp ggml releases](https://huggingface.co/ggerganov/whisper.cpp)) and pick them in the app:

| Size | Typical filename |
|------|------------------|
| tiny | `ggml-tiny.bin` |
| base | `ggml-base.bin` |
| small | `ggml-small.bin` |

The app never downloads models and never commits `*.bin` files. Missing model paths fail clearly.

## Offline ASR details

- ABI: **arm64-v8a** only
- Engine: pinned **whisper.cpp v1.7.5** under `third_party/whisper.cpp`
- Input: 16 kHz mono float PCM after on-device decode (`MediaExtractor` / `MediaCodec`, plus WAV parsing)
- Formats: mp3, wav, m4a, flac, aac, ogg
- API: `TranscriptionEngine.loadModel` / `transcribeUri` / `transcribePcm`

## Build prerequisites

- JDK 17+
- Android SDK Platform 35, Build-Tools 35+
- Android NDK 26.1.10909125 (pinned in `app/build.gradle.kts`) and CMake 3.22.1
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
| `third_party/whisper.cpp/` | Vendored whisper.cpp v1.7.5 |

## License

MIT — see [LICENSE](LICENSE). whisper.cpp is MIT (see `third_party/whisper.cpp/LICENSE`).
