# Clip to MP3 — first version

## Goal and agreed scope
Build a personal Android app for converting mostly short MP4 clips into real MP3 audio. The user will do everything from their phone, so delivery must include an installable APK; source code alone does not meet the goal. The user approved the one-screen, offline design on 18 October 2026.

## Phone experience
1. Tap **Choose video** and select one MP4 using Android's system file picker.
2. See its filename and tap **Convert to MP3**.
3. See conversion progress and a **Cancel** control. Keep the screen awake during conversion and tell the user to keep the app open.
4. When complete, tap **Save MP3** to choose a destination, or **Share** to use Android's share sheet.

Use the original filename with an `.mp3` extension as the suggested name. Use fixed 192 kbps output, retaining mono or stereo as appropriate. Conversion does not improve the source recording's quality. No account, advertising, analytics, internet permission, batch conversion, trimming, or quality settings.

## Architecture
Create a small native Android application with a Java UI and Android platform widgets. Support Android 10 and later, including ARM64 phones. Use Android's MediaExtractor and MediaCodec to select the first supported audio track and decode it into PCM. Encode PCM with a bundled LAME library through a narrow JNI interface. Keep UI, decoding, encoding, and file export in separate components.

Conversion runs on a worker thread and streams bounded buffers to a private temporary MP3; it must not load the whole video or decoded audio into memory. Progress comes from decoded timestamps. Feed the encoder only complete PCM frames, handle decoder format changes explicitly, and flush the encoder before reporting completion. Handle mono/stereo input; reject unsupported channel layouts or decoder output formats with a readable message.

Use Android content URIs throughout file selection and export. Sharing uses a content provider with temporary read access. Save copies the completed temporary MP3 to a system-picked document. Do not request broad storage access. Retain completed output for retry if saving fails. Delete partial output on cancel/error and stale private temporary files on the next launch. Preserve active conversion across screen rotation; cancel cleanly if the process is destroyed. Background conversion and recovery after process death are outside this version.

## Failures and privacy
Show clear messages for no audio, unsupported audio, unreadable or damaged input, insufficient space, and failed export. Cancellation must release codecs, streams, and encoder resources. The source video is never modified. All media processing happens on the phone; build-time downloads do not imply app network access.

## Build and delivery
Set up Android SDK/build tools and the native compiler in the build workspace. Pin toolchain and encoder versions when preparing the implementation plan. Bundle encoder license notices and make the corresponding source and rebuild instructions available with the source archive. Deliver a signed personal-use APK, a source archive, and brief phone installation instructions. Preserve signing material privately for future updates; never include private signing material in the source archive.

The workspace has Java but no Android SDK installed. Android download endpoints responded successfully during feasibility checks. A successful APK build is still unverified and must not be represented as complete until packaging and signing checks pass.

## Validation and acceptance
- Build the APK, verify its signature and manifest, and confirm the package contains the expected ARM64 encoder library.
- Validate MP3 output using known mono and stereo sample audio, checking playable format, approximate duration, and presence of non-silent samples.
- Exercise the Android conversion flow in an emulator if available: normal clip, video without audio, cancellation, rotation, and save/share access.
- Check error paths for corrupted input and export failure; confirm partial temporary output is removed.
- Clearly report any emulator or physical-device testing that could not be completed. Final acceptance requires the user to install it on their phone and try a short clip; do not claim that their phone has been tested remotely.

## Design review
The scope is intentionally one clip at a time with fixed output quality. The main implementation risks are codec format handling, native library packaging, and producing the APK within this environment. None changes the agreed user flow. Implementation follows review of this written specification.
