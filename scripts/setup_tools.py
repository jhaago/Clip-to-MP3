#!/usr/bin/env python3
"""Download pinned upstream build tools and preserve archive symbolic links."""
import concurrent.futures,hashlib,os,pathlib,stat,urllib.request,zipfile
TOOLS=pathlib.Path(os.environ.get('CLIP_ANDROID_TOOLS','/workspace/android-tools'));TOOLS.mkdir(parents=True,exist_ok=True)
PACKAGES=[
 ('platform.zip','platform-35_r02.zip','0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0'),
 ('build-tools.zip','build-tools_r35_linux.zip','bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88'),
 ('ndk.zip','android-ndk-r27c-linux.zip','59c2f6dc96743b5daf5d1626684640b20a6bd2b1d85b13156b90333741bad5cc'),
]
def setup(item):
 name,url,expected=item;archive=TOOLS/name
 if not archive.exists():urllib.request.urlretrieve('https://dl.google.com/android/repository/'+url,archive)
 with archive.open('rb') as f:actual=hashlib.file_digest(f,'sha256').hexdigest()
 if actual!=expected:raise RuntimeError('Checksum mismatch: '+name)
 with zipfile.ZipFile(archive) as z:
  for info in z.infolist():
   path=TOOLS/info.filename
   if stat.S_ISLNK(info.external_attr>>16):
    path.unlink(missing_ok=True);path.symlink_to(z.read(info).decode())
   elif not path.exists():
    z.extract(info,TOOLS)
    if info.external_attr>>16:os.chmod(path,info.external_attr>>16)
 print('Ready: '+url,flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:list(pool.map(setup,PACKAGES))
