# Changelog

All notable changes to WhisperVault are documented in this file.

## [Unreleased]

### Added

- Transcript export as `.txt`, `.json`, and `.srt`
- MediaStore writes under `Download/WhisperVault/` (no broad storage permission)
- SRT cues built from whisper.cpp segment timestamps (`t0`/`t1` centiseconds)
- Auto-export when a queue task reaches DONE; manual re-export from the queue list

### Earlier

- Batch queue (Room + WorkManager + foreground `dataSync`)
- Offline whisper.cpp ASR
- SAF audio picker and project scaffold
