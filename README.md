# Thaumic Tinkerer

[简体中文](README.zh-CN.md)

Craft specialized tools and enchantments, then progress through infusion to unlock Ichor equipment.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.13 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `0.4.116.2+`.

Thaumcraft itself also requires Curios API and TerraBlender.

Optional integrations include CC: Tweaked `1.113.1+`, Ender Storage `[2.11.0.188,2.12)`, Jade `[11.13.1,12)`, Botania, Tinkers’ Construct. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

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

The installable JAR is `build/libs/[TC4Continuation]Thaumic-Tinkerer-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

For development runs with extra integration mods, add `--profile integrations` to the preparation command.

To use series mods built from source, put one main JAR for each of `thaumcraft`, `thaumicbases` in one folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.
