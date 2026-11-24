# Thaumic Omniverse

[简体中文](README.zh-CN.md)

Tune magical equipment, route essentia and use Create to build arcane airships that carry cargo along scheduled routes.

Void Sand Paper has 64 durability. In the server configuration, enable `preserve_fake_player_void_tools` for wear-free void tools in deployers and animation tablets. Enable `repair_deployer_tools` to let a wand charger repair Repair-enchanted tools held by deployers. Both options default to false.

A Chaos Matrix Thaumatorium displays the essentia needed by a Create sequenced assembly crucible step. Once supplied, it advances the intermediate item to its next step.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `4.3+`.

Thaumcraft itself also requires Curios API and TerraBlender.

Optional integrations include Jade, Create `6.0.0+`, Botania, Tinkers’ Construct, JourneyMap, Productive Bees, Thaumic Futures `0.2.10.2+`. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

## Build

Install JDK 17 and Python 3.8 or later and point `JAVA_HOME` at the JDK. Run these commands from the repository root. The wrapper uses Gradle 8.8 and compiles against Forge 47.4.23.

Windows (PowerShell)

```powershell
py -3 dependencies/prepare.py
.\gradlew.bat assemble
```

Linux / macOS

```sh
python3 dependencies/prepare.py
./gradlew assemble
```

The installable JAR is `build/libs/[TC4Continuation]Thaumic-Omniverse-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

For development runs with extra integration mods, add `--profile integrations` to the preparation command.

For Create 0.5.1.j, add `--profile create051` to the preparation command, then run the wrapper with `assemble -Pcreate051`. The compatibility JAR ends in `-create051.jar` under `build-create051/libs/`.

To use series mods built from source, put one main JAR for each of `thaumcraft`, `thaumicfutures` in one folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.

The full regression suite also reads Thaumcraft research and aspect data from the development workspace.
