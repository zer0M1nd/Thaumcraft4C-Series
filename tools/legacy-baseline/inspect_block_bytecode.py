"""Read existing 1.7.10 bytecode without loading game classes or launching a client.

Only writes ignored evidence files inside this checkout. No third-party Python packages.
"""
import argparse
import lzma
import pathlib
import re
import subprocess
import zipfile


def main():
    root = pathlib.Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--client-jar", type=pathlib.Path, default=pathlib.Path(
        r"E:\MC\Minecraft 1.7.10 Thaumcraft Only\.minecraft\versions\1.7.10\1.7.10.jar"))
    parser.add_argument("--javap", default=r"D:\Program Files\Java\jdk-17.0.5\bin\javap.exe")
    args = parser.parse_args()
    destination = root / ".tooling/legacy-evidence"
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(root / ".tooling/legacy-server/forge.jar") as archive:
        mappings = lzma.decompress(archive.read("deobfuscation_data-1.7.10.lzma")).decode()
    (destination / "joined.srg").write_text(mappings, encoding="utf-8")
    classes = {}
    selected = {}
    for line in mappings.splitlines():
        fields = line.split()
        if fields[0] == "CL:":
            classes[fields[1]] = fields[2]
        elif fields[0] == "MD:" and fields[3].endswith(("/func_149694_d", "/func_149643_k", "/func_149648_K")):
            owner, method = fields[1].split("/")
            selected.setdefault(owner, set()).add(method)
    evidence = []
    for owner in sorted(selected):
        output = subprocess.check_output([args.javap, "-c", "-p", "-classpath", str(args.client_jar), owner], text=True)
        for chunk in re.split(r"\n(?=  (?:public|private|protected|static))", output):
            signature = chunk.split("\n")[0]
            if any((f" {method}(ahb, int, int, int);" in signature) or (method == "K" and " K();" in signature)
                   for method in selected[owner]):
                evidence.append(classes[owner] + ":\n" + chunk)
    target = destination / "block-picking-client-bytecode.txt"
    target.write_text("\n".join(evidence), encoding="utf-8")
    tc_jar = root / ".tooling/legacy-server/mods/Thaumcraft-1.7.10-4.2.3.5.jar"
    output = subprocess.check_output([args.javap, "-c", "-p", "-classpath", str(tc_jar),
                                      "thaumcraft.common.lib.utils.BlockUtils"], text=True)
    (destination / "BlockUtils.javap.txt").write_text(output, encoding="utf-8")
    print(f"Inspected {len(selected)} vanilla classes; evidence: {target}")


if __name__ == "__main__":
    main()
