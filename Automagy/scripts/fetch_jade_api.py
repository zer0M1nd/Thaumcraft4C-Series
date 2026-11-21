"""Restore the compile-only official Jade artifact; it is never bundled."""
from pathlib import Path
import urllib.request,hashlib
URL = 'https://cdn.modrinth.com/data/nvQzSEkH/versions/xJQHCmWJ/Jade-1.20.1-Forge-11.13.3.jar'
SHA512 = '72025c4ad6d26522bafe3d5d6ac9d4702c006a868312cb34df8a7a2398a4618205839607b129c167fd176e58cb7655f08e3afbf1993925c80d4fcc559c45a037'
DEST = Path(__file__).resolve().parents[1] / '.deps' / 'Jade-1.20.1-Forge-11.13.3.jar'
if not DEST.exists() or hashlib.sha512(DEST.read_bytes()).hexdigest()!=SHA512:
    data=urllib.request.urlopen(URL,timeout=60).read()
    if hashlib.sha512(data).hexdigest()!=SHA512: raise RuntimeError('Jade artifact hash mismatch')
    DEST.parent.mkdir(parents=True,exist_ok=True);DEST.write_bytes(data)
print(DEST)
