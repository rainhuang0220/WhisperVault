# Changelog

All notable changes to WhisperVault are documented in this file.

## [Unreleased]

### Added

- Batch transcription queue designed for up to 100 files
- Room persistence for task states: WAITING, RUNNING, DONE, FAILED
- WorkManager unique work with expedited/long-running foreground execution (`dataSync`)
- Foreground notification so transcription continues while the screen is locked
- Process-death recovery: unfinished Room tasks are resumed on app start
- Home UI queue list with live status

### Earlier

- Offline whisper.cpp ASR (JNI, decode, ggml tiny/base/small)
- SAF multi-select audio picker
- Android Gradle / Compose scaffold
