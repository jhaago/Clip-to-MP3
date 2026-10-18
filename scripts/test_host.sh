#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p tmp/host-lame tmp/test-classes
if [ ! -f tmp/host-lame/libmp3lame/.libs/libmp3lame.a ]; then
  (cd tmp/host-lame && ../../third_party/lame/configure --disable-shared --enable-static --disable-frontend --disable-decoder --with-pic > configure.log 2>&1 && make -j4 > make.log 2>&1)
fi
gcc -O2 -I third_party/lame/include tests/encoder_harness.c tmp/host-lame/libmp3lame/.libs/libmp3lame.a -lm -o tmp/encoder-harness
python3 tests/test_encoder.py
java com.sun.tools.javac.Main -d tmp/test-classes app/src/main/java/com/personal/cliptomp3/PcmFrames.java app/src/main/java/com/personal/cliptomp3/OutputNames.java tests/CoreTest.java
java -cp tmp/test-classes com.personal.cliptomp3.CoreTest
task_tools="${CLIP_ANDROID_TOOLS:-/workspace/android-tools}"
task_platform="${CLIP_ANDROID_PLATFORM:-$task_tools/android-35}"
java com.sun.tools.javac.Main --release 8 -cp "$task_platform/android.jar" -d tmp/selection-test \
  tests/stubs/android/app/Fragment.java tests/stubs/android/os/Handler.java tests/stubs/android/os/Looper.java tests/SelectionTest.java \
  app/src/main/java/com/personal/cliptomp3/ConversionController.java \
  app/src/main/java/com/personal/cliptomp3/AudioConverter.java \
  app/src/main/java/com/personal/cliptomp3/Mp3Encoder.java \
  app/src/main/java/com/personal/cliptomp3/PcmFrames.java \
  app/src/main/java/com/personal/cliptomp3/OutputFiles.java
java -cp "tmp/selection-test:$task_platform/android.jar" com.personal.cliptomp3.SelectionTest
task_ndk="${CLIP_ANDROID_NDK:-$task_tools/android-ndk-r27c}"
mkdir -p tmp/host-jni
cp "$task_ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/jni.h" tmp/host-jni/jni.h
gcc -D__ANDROID__ -O2 -fPIC -shared -I tmp/host-jni -I third_party/lame/include \
  app/src/main/cpp/encoder.c tmp/host-lame/libmp3lame/.libs/libmp3lame.a -lm -o tmp/host-jni/libclipmp3.so
java com.sun.tools.javac.Main --release 8 -d tmp/host-jni app/src/main/java/com/personal/cliptomp3/Mp3Encoder.java tests/JniTest.java
java -Djava.library.path=tmp/host-jni -cp tmp/host-jni com.personal.cliptomp3.JniTest tmp/host-jni/output.mp3
python3 - <<'PY'
import json,subprocess
data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json','tmp/host-jni/output.mp3']))
assert data['streams'][0]['codec_name']=='mp3'
assert data['streams'][0]['channels']==2
assert abs(float(data['format']['duration'])-1)<0.15
print('JNI output verified as playable stereo MP3')
PY
