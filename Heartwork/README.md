# The Heartwork

[简体中文](README.zh-CN.md)

Forge sabers, grow crops for new dishes and use infusion to unlock named blades and their special moves.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.23 or later in the 47.x series.

Required mods include Thaumcraft 4: Continuation `0.4.121+`.

Thaumcraft itself also requires Curios API and TerraBlender.

Optional integrations include SlashBlade: Resharped `[1.9.65,1.10)`, Industrial Craft 2 `[2.10.33-ex120,2.11)`, Botania `[1.20.1-456,1.20.2)`, exparmory. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

**On first launch, the resource download screen retrieves the original 1.7.10 FIX JAR from the project website.** Thaumcraft verifies its SHA-256 and extracts the required resources. For manual import, download a copy, place it in the active instance’s `thaumcraft/` directory and select **Retry**. The loader removes the input JAR after extraction. [Download the Heartwork FIX resource JAR](http://tc4c.zer0m1nd.me/download/files/original-resources/heartwork/HeartWork_FIX_MC1.7.10.jar)

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

The installable JAR is `build/libs/[TC4Continuation]The-Heartwork-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

For development runs with extra integration mods, add `--profile integrations` to the preparation command.

To use series mods built from source, place the main Thaumcraft JAR (`thaumcraft`) in a folder. With Python 3.11 or later, run `python3 dependencies/prepare.py --series-dir <folder>` (`py -3` on Windows), then use the wrapper command it prints.
