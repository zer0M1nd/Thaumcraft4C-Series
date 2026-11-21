# Thaumic Exploration: Continuation

[简体中文](README.zh-CN.md)

Meditate in an enchanted vat, link distant inventories and craft wands and boots with new abilities.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.23 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `0.4.116.2+`, Curios API `5.0+`.

Thaumcraft itself also requires Curios API and TerraBlender.

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

The installable JAR is `build/libs/[TC4Continuation]Thaumic-Exploration-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

To use series mods built from source, place the main Thaumcraft JAR (`thaumcraft`) in a folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.
