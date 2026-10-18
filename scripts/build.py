#!/usr/bin/env python3
"""Build with Android's SDK directly: no Android Studio or Maven required."""
import json, os, pathlib, secrets, shutil, subprocess, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
TOOLS=pathlib.Path(os.environ.get('CLIP_ANDROID_TOOLS','/workspace/android-tools'))
SDK=pathlib.Path(os.environ.get('CLIP_ANDROID_PLATFORM',TOOLS/'android-35'))
BT=pathlib.Path(os.environ.get('CLIP_ANDROID_BUILD_TOOLS',TOOLS/'android-15'))
NDK=pathlib.Path(os.environ.get('CLIP_ANDROID_NDK',TOOLS/'android-ndk-r27c'))
BUILD=ROOT/'build'; BUILD.mkdir(exist_ok=True)
def run(*args,**kw):
    subprocess.run([str(a) for a in args],check=True,**kw)
def build_native(abi,host):
    folder=BUILD/('native-'+abi);folder.mkdir(exist_ok=True)
    llvm=NDK/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    static=folder/'libmp3lame/.libs/libmp3lame.a'
    if not static.exists():
        with (folder/'build.log').open('w') as log:
            run(ROOT/'third_party/lame/configure','--host='+host,'--disable-shared','--enable-static','--disable-frontend','--disable-decoder','--disable-gtktest','--with-pic','CC='+str(llvm/(host+'29-clang')),'AR='+str(llvm/'llvm-ar'),'RANLIB='+str(llvm/'llvm-ranlib'),cwd=folder,stdout=log,stderr=log)
            run('make','-j4',cwd=folder,stdout=log,stderr=log)
    out=BUILD/'lib'/abi;out.mkdir(parents=True,exist_ok=True)
    run(llvm/(host+'29-clang'),'-shared','-fPIC','-O2','-Wl,-z,max-page-size=16384','-Wl,-z,relro,-z,now','-I'+str(ROOT/'third_party/lame/include'),ROOT/'app/src/main/cpp/encoder.c',static,'-lm','-o',out/'libclipmp3.so')
    run(llvm/'llvm-strip','--strip-unneeded',out/'libclipmp3.so')
build_native('arm64-v8a','aarch64-linux-android')
build_native('x86_64','x86_64-linux-android')
classes=BUILD/'classes';classes.mkdir(exist_ok=True)
java=list((ROOT/'app/src/main/java').rglob('*.java'))
run('java','com.sun.tools.javac.Main','--release','8','-classpath',SDK/'android.jar','-d',classes,*java)
jar=BUILD/'classes.jar'
with zipfile.ZipFile(jar,'w') as z:
    for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
dex=BUILD/'dex';dex.mkdir(exist_ok=True)
run('java','-cp',BT/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','29','--lib',SDK/'android.jar','--output',dex,jar)
resources=BUILD/'resources.zip'
run(BT/'aapt2','compile','--dir',ROOT/'app/src/main/res','-o',resources)
assets=BUILD/'assets';assets.mkdir(exist_ok=True)
shutil.copyfile(ROOT/'third_party/lame/COPYING',assets/'LAME-COPYING.txt')
base=BUILD/'base.apk'
run(BT/'aapt2','link','-I',SDK/'android.jar','--manifest',ROOT/'app/src/main/AndroidManifest.xml','--min-sdk-version','29','--target-sdk-version','35','-A',assets,'-o',base,resources)
with zipfile.ZipFile(base,'a',compression=zipfile.ZIP_DEFLATED) as z:
    z.write(dex/'classes.dex','classes.dex')
    for f in (BUILD/'lib').rglob('*.so'):z.write(f,f.relative_to(BUILD))
aligned=BUILD/'aligned.apk'
run(BT/'zipalign','-f','-P','16','4',base,aligned)
private=pathlib.Path(os.environ.get('CLIP_SIGNING_DIR','/workspace/android-private'));private.mkdir(mode=0o700,exist_ok=True)
metadata=private/'signing.json';key=private/'clip-to-mp3.jks'
if not metadata.exists():
    metadata.write_text(json.dumps({'password':secrets.token_urlsafe(32)}));metadata.chmod(0o600)
env=os.environ.copy();env['CLIP_STORE_PASSWORD']=json.loads(metadata.read_text())['password']
if not key.exists():
    run('keytool','-genkeypair','-keystore',key,'-alias','clip-to-mp3','-keyalg','RSA','-keysize','3072','-validity','10000','-storepass:env','CLIP_STORE_PASSWORD','-keypass:env','CLIP_STORE_PASSWORD','-dname','CN=Clip to MP3 Personal App',env=env)
    key.chmod(0o600)
dist=ROOT/'dist';dist.mkdir(exist_ok=True);apk=dist/'Clip-to-MP3.apk'
signed=BUILD/('signed-'+secrets.token_hex(8)+'.apk')
run('java','-jar',BT/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','clip-to-mp3','--ks-pass','env:CLIP_STORE_PASSWORD','--key-pass','env:CLIP_STORE_PASSWORD','--v4-signing-enabled','false','--out',signed,aligned,env=env)
run('java','-jar',BT/'lib/apksigner.jar','verify','--verbose',signed)
run(BT/'zipalign','-c','-P','16','4',signed)
with zipfile.ZipFile(signed) as z:
    if z.testzip() is not None:raise RuntimeError('APK ZIP integrity verification failed')
os.replace(signed,apk)
# Incremental-install metadata from earlier builds must never accompany a new APK.
pathlib.Path(str(apk)+'.idsig').unlink(missing_ok=True)
print('Built '+str(apk))
