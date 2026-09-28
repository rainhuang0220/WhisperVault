# Whisper.cpp (pinned v1.7.5)

Vendored under `third_party/whisper.cpp` (MIT, Georgi Gerganov et al.).

Android NDK builds `libwhispervault.so` for **arm64-v8a** from:
- `third_party/whisper.cpp/src/whisper.cpp`
- `third_party/whisper.cpp/ggml/**` (CPU backend)
- `app/src/main/cpp/whispervault_jni.cpp`

Do not commit ggml `*.bin` model weights.
