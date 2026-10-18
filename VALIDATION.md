# Validation record

## Verified build properties

- Native Android app: package `com.personal.cliptomp3`, version 1.0 (code 1).
- Minimum Android API 29 (Android 10), target/compile API 35.
- ARM64 and x86_64 LAME libraries; ELF load segments aligned to 16 KB.
- Signed APK verified with Android `apksigner`; ZIP integrity and 16 KB APK alignment checked independently after packaging.
- No internet or broad storage permissions in the manifest.

## Host checks

`bash scripts/test_host.sh` passed:

- Real MP3 encoding from mono and stereo PCM at 16, 44.1 and 48 kHz.
- ffprobe confirms MP3 codec, correct channel count, 192 kbps and approximate duration; decoding confirms non-silent audio.
- The actual Java/JNI wrapper encodes playable stereo MP3, flushes correctly, rejects invalid channels and rejects use after finish/close.
- Split PCM frames, float conversion/clamping, incomplete frames and unsupported channels.
- Filename suggestions including uppercase extensions and Unicode.
- Selecting a new input clears the previous result (regression observed failing before the fix).

The controller host regression uses minimal Android lifecycle/handler stubs. It tests the real selection method; it does not simulate Android rotation or codec behavior.

## Android checks

An Android 10 x86_64 software emulator has booted. A separate instrumentation APK compiles and includes twelve checks: mono/stereo MP4 conversion through nonseekable content URIs, no-audio and damaged videos, cancellation, stalled-provider cancellation, export failure retention, selecting another video, duplicate Convert taps, rotation, cancelled Save, and cross-UID sharing. The first APK installation succeeded after an unsupported incremental-install attempt fell back to normal installation. The automated conversion suite did not complete: the software emulator/ADB connection stalled and a subsequent boot-property query timed out. No Android end-to-end pass is claimed. Rotation, share grants, stalled-provider cancellation and actual on-device codec conversion still need runtime verification; their test sources compile. Physical-device testing was not available.

## Review and corrections

A fresh code review identified stale output after selecting a new input, blocking staging cancellation, and a corrupt intermediate APK. The selection regression now passes. Input staging uses a cancellable provider open and timed descriptor polling. APK publication now signs to a fresh temporary file, verifies signature/CRC/alignment, then atomically replaces the deliverable; stale incremental-install metadata is removed.

The build uses the pinned Android SDK and NDK directly instead of Gradle/CMake. The source archive includes the exact build scripts. Physical-phone testing remains the user's final installation check.
