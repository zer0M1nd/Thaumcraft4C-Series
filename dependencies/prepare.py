"""Prepare the local inputs listed in artifacts.json. Requires Python 3.8+."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile


def checksum(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--from-dir", type=Path, action="append", default=[],
                        help="also look here for previously obtained JARs")
    parser.add_argument("--offline", action="store_true", help="use local files only")
    parser.add_argument("--all", action="store_true", help="include optional development profiles")
    parser.add_argument("--profile", action="append", default=[],
                        help="also prepare a named profile from artifacts.json")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    records = json.loads((directory / "artifacts.json").read_text(encoding="utf-8"))
    artifacts = {record["file"]: record for record in records}
    profiles = {record["profile"] for record in records if "profile" in record}
    if set(args.profile) - profiles:
        parser.error("available profiles: " + ", ".join(sorted(profiles)))
    dest = directory / "jars"
    dest.mkdir(exist_ok=True)
    prepared = set()

    def prepare(name):
        if name in prepared:
            return dest / name
        record = artifacts[name]
        expected = record["sha256"]
        target = dest / name
        if target.exists():
            if checksum(target) != expected:
                raise ValueError(f"{target} has different bytes. Move it aside and retry.")
        else:
            original = record.get("upstreamFile", name)
            local = next((folder / candidate for folder in args.from_dir
                          for candidate in (name, original) if (folder / candidate).is_file()
                          and checksum(folder / candidate) == expected), None)
            with tempfile.NamedTemporaryFile(dir=dest, delete=False) as scratch:
                temp = Path(scratch.name)
            try:
                if local:
                    shutil.copyfile(local, temp)
                elif "parent" in record:
                    with zipfile.ZipFile(prepare(record["parent"])) as archive:
                        with archive.open(record["entry"]) as src, temp.open("wb") as output:
                            shutil.copyfileobj(src, output)
                elif args.offline:
                    raise FileNotFoundError(f"Provide {original} via --from-dir. Expected SHA-256 {expected}")
                else:
                    req = urllib.request.Request(record["url"], headers={"User-Agent": "TC4Continuation-build/1"})
                    with urllib.request.urlopen(req, timeout=90) as src, temp.open("wb") as output:
                        shutil.copyfileobj(src, output)
                if checksum(temp) != expected:
                    raise ValueError(f"SHA-256 mismatch for {original}. Expected {expected}")
                os.replace(temp, target)
            finally:
                if temp.exists():
                    temp.unlink()
        prepared.add(name)
        return target

    for record in records:
        if args.all or "profile" not in record or record["profile"] in args.profile:
            try:
                prepare(record["file"])
            except (OSError, ValueError, KeyError, zipfile.BadZipFile, urllib.error.URLError) as exc:
                sys.exit(f"{record['file']}: {exc}")
    print(f"Verified {len(prepared)} local inputs in {dest}")


if __name__ == "__main__":
    main()
