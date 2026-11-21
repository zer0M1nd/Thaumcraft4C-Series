"""Resolve the four runtime post chains without opening a Minecraft/OpenGL client.

Checks resource names, post targets, program stages and configured uniform overrides.
This cannot prove GLSL compilation, GPU linking or visual parity.
"""
import json
from pathlib import Path
import re
import argparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MINECRAFT = ROOT / ".tooling/gradle-user-home/caches/forge_gradle/mcp_repo/de/oceanlabs/mcp/mcp_config"
JARS = list(MINECRAFT.glob("1.20.1-*/joined/downloadClient/client.jar"))


def resource(name, directory, extension):
    namespace, path = name.split(":", 1) if ":" in name else ("minecraft", name)
    if not re.fullmatch(r"[a-z0-9_.-]+", namespace) or not re.fullmatch(r"[a-z0-9/._-]+", path):
        raise ValueError(f"Invalid resource name: {name}")
    return f"assets/{namespace}/{directory}/{path}{extension}"


def verify(built=False):
    sources = [ROOT / "build/resources/main"] if built else [ROOT / "src/main/resources", ROOT / "src/generated/resources"]
    if any(not root.is_dir() for root in sources):
        raise ValueError("Required resource directory is missing; generate/process resources first")
    if len(JARS) != 1:
        raise ValueError(f"Expected one Minecraft 1.20.1 client asset dependency, found {len(JARS)}")
    with zipfile.ZipFile(JARS[0]) as minecraft:
        def read(path):
            for root in reversed(sources):
                file = root / path
                if file.is_file():
                    return file.read_text(encoding="utf-8-sig")
            return minecraft.read(path).decode("utf-8-sig")

        # Scan all shipped shader resource names, including programs not used by a current effect.
        for root in sources:
            for file in root.glob("assets/*/shaders/**/*"):
                if file.is_file():
                    relative = file.relative_to(root).as_posix()
                    if not re.fullmatch(r"[a-z0-9/._-]+", relative):
                        raise ValueError(f"Invalid shipped shader path: {relative}")
        runtime = (ROOT / "src/main/java/tc4revival/client/WarpPostEffects.java").read_text(encoding="utf-8-sig")
        names = re.findall(r'"([a-z0-9_]+)"', re.search(r"NAMES\s*=\s*\{([^}]+)\}", runtime)[1])
        programs, stages, passes = set(), set(), 0
        for name in names:
            chain = json.loads(read(resource(f"thaumcraft:{name}", "shaders/post", ".json")))
            targets = {"minecraft:main"} | {t if isinstance(t, str) else t["name"] for t in chain.get("targets", [])}
            for step in chain["passes"]:
                passes += 1
                for field in ("intarget", "outtarget"):
                    if step[field] not in targets:
                        raise ValueError(f"{name}: unknown {field} {step[field]}")
                path = resource(step["name"], "shaders/program", ".json")
                program = json.loads(read(path))
                programs.add(path)
                for field, extension in (("vertex", ".vsh"), ("fragment", ".fsh")):
                    stage = resource(program[field], "shaders/program", extension)
                    if not read(stage).strip():
                        raise ValueError(f"Empty stage: {stage}")
                    stages.add(stage)
                uniforms = {u["name"]: u for u in program.get("uniforms", [])}
                for override in step.get("uniforms", []):
                    uniform = uniforms.get(override["name"])
                    if uniform is None or len(override["values"]) != uniform["count"]:
                        raise ValueError(f"{name}: invalid uniform override {override['name']}")
        print(f"PASS: {len(names)} post chains, {passes} passes, {len(programs)} programs, {len(stages)} stages; resource names, dependencies, targets and uniform overrides resolve.")
        print("Static validation only; GLSL compilation and client appearance remain unverified.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--built", action="store_true", help="Validate processResources output instead of source resources")
    verify(parser.parse_args().built)
