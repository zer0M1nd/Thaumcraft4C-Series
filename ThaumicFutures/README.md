# Thaumic Futures

[简体中文](README.zh-CN.md)

Wield grappling and building foci from Thaumcraft 5 and rediscover Thaumcraft 6 devices and classic cross-mod recipes.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `0.4.119.3+`.

Thaumcraft itself also requires Curios API and TerraBlender.

Optional integrations include Create, Botania, Tinkers’ Construct, JourneyMap, Productive Bees. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

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

The installable JAR is `build/libs/[TC4Continuation]Thaumic-Futures-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

For development runs with extra integration mods, add `--profile integrations` to the preparation command.

To use series mods built from source, place the main Thaumcraft JAR (`thaumcraft`) in a folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.

The full regression suite also reads the historical Thaumcraft 0.4.68 JAR and sibling research data.
