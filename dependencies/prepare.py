"""Prepare the local inputs listed in artifacts.json. Requires Python 3.8+."""
import argparse
import hashlib
import json
import os
import re
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


def source_builds(directory, modids):
    """Index installable series JARs by Forge mod ID, independent of archive names."""
    try:
        import tomllib
    except ImportError:
        raise ValueError("--series-dir requires Python 3.11 or later")
    if not directory.is_dir():
        raise ValueError(f"Missing series JAR directory: {directory}")
    found = {}
    for path in sorted(directory.glob("*.jar")):
        if path.name.endswith(("-sources.jar", "-create051.jar")):
            continue
        with zipfile.ZipFile(path) as archive:
            if "META-INF/mods.toml" not in archive.namelist():
                continue
            meta = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8-sig"))
            for mod in meta.get("mods", []):
                modid = mod["modId"]
                if modid not in modids:
                    continue
                version = mod["version"]
                if not re.fullmatch(r"[0-9]+(?:\.[0-9]+){2,3}", version):
                    raise ValueError(f"Expected a built series version in {path}: {version}")
                if modid in found:
                    raise ValueError(f"Provide one main JAR for {modid}: {found[modid][0]} and {path}")
                found[modid] = (path, version)
    missing = modids - found.keys()
    if missing:
        raise ValueError("Missing series main JARs: " + ", ".join(sorted(missing)))
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--from-dir", type=Path, action="append", default=[],
                        help="also look here for previously obtained JARs")
    parser.add_argument("--offline", action="store_true", help="use local files only")
    parser.add_argument("--all", action="store_true", help="include optional development profiles")
    parser.add_argument("--profile", action="append", default=[],
                        help="also prepare a named profile from artifacts.json")
    parser.add_argument("--series-dir", type=Path,
                        help="use your own built series JARs (Python 3.11+) and print the wrapper command")
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

    override_args = []
    if args.series_dir:
        selected = [r for r in records if "modId" in r and
                    (args.all or "profile" not in r or r["profile"] in args.profile)]
        try:
            local_series = source_builds(args.series_dir, {r["modId"] for r in selected})
        except (OSError, ValueError, zipfile.BadZipFile) as exc:
            sys.exit(str(exc))
        for record in selected:
            path, version = local_series[record["modId"]]
            digest = checksum(path)
            # Include the content ID so ForgeGradle remaps different local builds separately.
            revision = version + "-" + digest[:16]
            stem = record["file"].rsplit("-", 1)[0]
            name = f"{stem}-{revision}.jar"
            replacement = dict(record, file=name, sha256=digest, upstreamFile=path.name)
            artifacts[name] = replacement
            record["file"] = name
            override_args.append("-P" + record["versionProperty"] + "=" + revision)
        args.from_dir.insert(0, args.series_dir)
    for record in records:
        if args.all or "profile" not in record or record["profile"] in args.profile:
            try:
                prepare(record["file"])
            except (OSError, ValueError, KeyError, zipfile.BadZipFile, urllib.error.URLError) as exc:
                sys.exit(f"{record['file']}: {exc}")
    print(f"Verified {len(prepared)} local inputs in {dest}")
    if override_args:
        wrapper = ".\\gradlew.bat" if os.name == "nt" else "./gradlew"
        print(" ".join([wrapper, "assemble"] + override_args))


if __name__ == "__main__":
    main()
