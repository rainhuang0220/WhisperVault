# Changelog

All notable changes to WhisperVault are documented in this file.

## [Unreleased]

### Changed

- Chunked MediaCodec decode and 30s overlapping whisper windows for long audio
- OOM during transcription marks tasks FAILED instead of crashing the worker
- Queue UI survives rotation via ViewModel; unfinished work recovers via Room + WorkManager

### Added

- JVM unit tests: task state machine, transcript formatting (txt/json/srt), AudioFile validation
- Instrumentation test for home screen launch (no model binary required)

### Earlier

- Export, queue, whisper.cpp ASR, SAF picker, scaffold
