#!/usr/bin/env python3
import json, os, pathlib, subprocess, zipfile, sys
ROOT=pathlib.Path(__file__).resolve().parents[1];BUILD=ROOT/'build';TOOLS=pathlib.Path(os.environ.get('CLIP_ANDROID_TOOLS','/workspace/android-tools'));BT=TOOLS/'android-15';SDK=TOOLS/'android-35'
def run(*args,**kw):return subprocess.run([str(a) for a in args],check=True,**kw)
classes=BUILD/'test-classes';classes.mkdir(exist_ok=True)
run('java','com.sun.tools.javac.Main','--release','8','-classpath',str(SDK/'android.jar')+os.pathsep+str(BUILD/'classes'),'-d',classes,ROOT/'tests/TestInstrumentation.java',ROOT/'tests/FixtureProvider.java')
jar=BUILD/'tests.jar'
with zipfile.ZipFile(jar,'w') as z:
    for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
dex=BUILD/'test-dex';dex.mkdir(exist_ok=True)
run('java','-cp',BT/'lib/d8.jar','com.android.tools.r8.D8','--min-api','29','--lib',SDK/'android.jar','--classpath',BUILD/'classes.jar','--output',dex,jar)
apk=BUILD/'tests-base.apk'
run(BT/'aapt2','link','-I',SDK/'android.jar','--manifest',ROOT/'tests/AndroidManifest.xml','-A',BUILD/'test-assets','-o',apk)
with zipfile.ZipFile(apk,'a',compression=zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
run(BT/'zipalign','-f','4',apk,BUILD/'tests-aligned.apk')
private=pathlib.Path(os.environ.get('CLIP_SIGNING_DIR','/workspace/android-private'));env=os.environ.copy();env['CLIP_STORE_PASSWORD']=json.loads((private/'signing.json').read_text())['password']
signed=BUILD/'tests.apk'
run('java','-jar',BT/'lib/apksigner.jar','sign','--ks',private/'clip-to-mp3.jks','--ks-pass','env:CLIP_STORE_PASSWORD','--out',signed,BUILD/'tests-aligned.apk',env=env)
if '--build-only' in sys.argv:raise SystemExit(0)
adb=TOOLS/'platform-tools/adb'
run(adb,'install','--no-streaming','--no-incremental','-r',ROOT/'dist/Clip-to-MP3.apk');run(adb,'install','--no-streaming','--no-incremental','-r',signed)
result=subprocess.check_output([str(adb),'shell','am','instrument','-w','com.personal.cliptomp3.tests/com.personal.cliptomp3.TestInstrumentation'],text=True)
print(result)
if 'FAIL' in result or 'PASS 12 Android' not in result:raise SystemExit(1)
