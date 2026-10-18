#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p dist
git archive --format=zip --prefix=Clip-to-MP3/ HEAD -o dist/Clip-to-MP3-source.zip
python3 - <<'PY'
from pathlib import Path
import os,zipfile
private=Path(os.environ.get('CLIP_SIGNING_DIR','/workspace/android-private'))
backup=Path('dist/Clip-to-MP3-update-signing-backup.zip')
with zipfile.ZipFile(backup,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for name in ('clip-to-mp3.jks','signing.json'):z.write(private/name,name)
backup.chmod(0o600)
PY
