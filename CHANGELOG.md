# Changelog

All notable changes to WhisperVault are documented in this file.

## [Unreleased]

### Added

- Storage Access Framework multi-select audio picker (mp3, wav, m4a, flac, aac, ogg)
- `AudioFile` metadata model (uri, display name, size, mime, duration when available)
- Home screen list of selected files with clear-selection action
- Selection held in `HomeViewModel` for the session

### Earlier

- Android Gradle project (Kotlin, Compose, Material3, minSdk 26, targetSdk 35)
- Package layout: `ui`, `viewmodel`, `data`, `storage`, `transcription`, `worker`
- Compose home screen with brand, privacy note, file-selection button, and idle status
- JNI CMake skeleton (`libwhispervault`) under `app/src/main/cpp/`
- MIT license, README, and `.gitignore` for Android / models / keystores
