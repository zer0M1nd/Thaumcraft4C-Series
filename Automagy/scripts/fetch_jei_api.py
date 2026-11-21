"""Fetch the pinned compile-only JEI API into this addon; never copied into release jars."""
from pathlib import Path
from urllib.request import urlopen
import hashlib

TARGET = Path(__file__).resolve().parents[1] / ".deps/jei-1.20.1-common-api-15.20.0.106.jar"
URL = "https://maven.blamejared.com/mezz/jei/jei-1.20.1-common-api/15.20.0.106/jei-1.20.1-common-api-15.20.0.106.jar"
SHA256 = "277ad6284671748a2b63ef2721e4f5a547cef0589522904003e6f859364badd8"
if not TARGET.exists() or hashlib.sha256(TARGET.read_bytes()).hexdigest() != SHA256:
    with urlopen(URL, timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise RuntimeError("JEI API checksum mismatch; dependency was not installed")
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_bytes(data)
print(TARGET)
