# Clip to MP3

A personal Android app for converting one short MP4 video into MP3. Processing happens entirely on your phone. No account, ads, analytics, uploads, internet permission or broad storage permissions.

## Install on your phone

1. Download `Clip-to-MP3.apk` and open the downloaded file.
2. If Android asks, allow the app you opened it with to install this APK.
3. Open **Clip to MP3**, select an MP4 and tap **Convert to MP3**.
4. Keep the app open during conversion, then tap **Save MP3** or **Share**.

Requires Android 10 or later and an ARM64 phone. The APK also includes x86_64 for emulator testing. Conversion is designed for short clips; background conversion, batches and trimming are not included. Input must have an audio track supported by your phone's decoder. Mono/stereo and signed 16-bit/float PCM output are supported. Unsupported layouts and midstream format changes show an error. MP3 encoding cannot improve the original audio quality.

The input is staged to private temporary storage for reliable support of file providers, so allow enough free storage for the video plus the MP3. The original video is never changed. Cancelled/failed conversion files are removed. Completed private output expires after 24 hours on a subsequent app launch; use Save to keep it. A failed or cancelled Save does not remove the completed private audio.

## Build from source

Linux x86_64 prerequisites: Python 3.11+, a Java 17 installation containing `jdk.compiler` and `keytool`, GCC, make, and unzip. Host verification also needs ffmpeg/ffprobe. No Android Studio, Gradle or Maven is needed: the scripts use Android SDK tools directly.

```bash
python3 scripts/setup_tools.py
bash scripts/test_host.sh
bash scripts/build.sh
```

The first command downloads Android platform 35 revision 2, Build Tools 35.0.0 and NDK r27c (27.2.12479018), about 790 MB compressed. It verifies their recorded SHA-256 checksums. The original LAME 3.100 source is included, unmodified. Original upstream tarball SHA-256: `ddfe36cab873794038ae2c1210557ad34857a4b6bdc515785d1da9e175b1da1e`.

`CLIP_ANDROID_TOOLS` changes the tools directory (default `/workspace/android-tools`). You can instead set `CLIP_ANDROID_PLATFORM`, `CLIP_ANDROID_BUILD_TOOLS` and `CLIP_ANDROID_NDK` to existing installations. The upstream Build Tools archive unpacks to the historical folder `android-15`; the `source.properties` inside identifies it as 35.0.0.

The APK is written to `dist/Clip-to-MP3.apk`. `CLIP_SIGNING_DIR` changes the private signing directory (default `/workspace/android-private`). First build generates a key there; subsequent builds reuse it. Keep this directory safe and reuse it for updates. Signing secrets are deliberately excluded from this source archive. A rebuild using a new key requires uninstalling the prior app first.

Native libraries support 16 KB memory pages. The script runs APK signature verification and ZIP alignment checks. License notices are available in the app's About dialog and below.

## Android instrumentation checks

With an Android 10+ emulator/device attached through adb, generate two-second mono/stereo AAC MP4 fixtures and a video-only fixture into `build/test-assets/` (see `scripts/make_fixtures.py`), then run `python3 scripts/test_android.py`. It builds a separately signed test APK, installs both, and runs the tests. The instrumentation APK is for development only and is not part of the user APK.

## Source and license

The app's own source is provided for personal modification and rebuilding. See `THIRD_PARTY_NOTICES.txt` and `third_party/lame/COPYING` for LAME's license. The corresponding LAME source, build scripts and JNI adapter are included so you can modify and rebuild the encoder. The APK contains no downloaded prebuilt MP3 encoder binary.
