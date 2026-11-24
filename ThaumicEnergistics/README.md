# Thaumic Energistics

[简体中文](README.zh-CN.md)

Store and transport essentia through an Applied Energistics 2 ME network to supply alchemy and infusion.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `0.4.119.3+`, Applied Energistics 2 `[15.4.8,16)`.

Thaumcraft itself also requires Curios API and TerraBlender.

With AE2 Wireless Terminals 15.3 on Forge, combine the wireless arcane crafting terminal with a universal terminal or another wireless terminal module. Switch to the arcane module to craft with the same ME network. Just Enough Items also supports recipe transfers. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

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

The installable JAR is `build/libs/[TC4Continuation]Thaumic-Energistics-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

To use series mods built from source, place the main Thaumcraft JAR (`thaumcraft`) in a folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.

The full regression suite also reads the development workspace’s reference artwork.
