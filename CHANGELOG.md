# Changelog

All notable changes to WhisperVault are documented in this file.

## [1.0.0] — 2026-09-28

First public release of WhisperVault — privacy-first offline audio transcription for Android.

### Features

- Select multiple audio files (mp3, wav, m4a, flac, aac, ogg) via Storage Access Framework
- Fully offline ASR with whisper.cpp (arm64-v8a); user-provided ggml tiny / base / small models
- Batch queue (up to 100) with Room + WorkManager + foreground service (`WAITING` / `RUNNING` / `DONE` / `FAILED`)
- Continues while the screen is locked; recovers unfinished work after process death
- Export `.txt`, `.json`, and `.srt` to `Download/WhisperVault/` via MediaStore
- No `INTERNET` permission; audio never leaves the device

### Reliability

- Chunked decode and windowed whisper inference for long audio
- OOM-safe failure path for queue tasks
- JVM unit tests for queue state machine, export formatting, and audio validation

### Fixed (found during release verification)

- Home screen is now a single scrollable list, so the queue stays reachable in landscape
- Removed the default WorkManager initializer (the app provides its own configuration); this was flagged by release lint
- Stripped `ACCESS_NETWORK_STATE`, which WorkManager merges in; the app uses no network constraints

### Release build

- `versionName` 1.0.0, `versionCode` 1
- Release APK is signed with the debug key for this open-source drop (no signing secrets are committed)

## Earlier development commits

Phased commits from scaffold through reliability testing (see git history).
