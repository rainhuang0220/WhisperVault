# Changelog

All notable changes to WhisperVault are documented in this file.

## [Unreleased]

### Added

- Offline whisper.cpp v1.7.5 (vendored under `third_party/whisper.cpp`) built for arm64-v8a via NDK/CMake
- JNI bridge: model load (`whisper_init_from_file_with_params`), `whisper_full`, segment text/timestamps
- `TranscriptionEngine` API for PCM and content URI transcription (no network)
- `AudioPreprocessor` decoding mp3/wav/m4a/flac/aac/ogg to 16 kHz mono float PCM
- Support for user-provided ggml tiny / base / small models (SAF → app files dir)
- Home screen model picker and engine version probe

### Earlier

- SAF multi-select audio picker and `AudioFile` metadata
- Android Gradle / Compose scaffold, package layout, MIT license
