# Thaumcraft Updater

[简体中文](README.zh-CN.md)

Choose which updates to apply to installed series mods before Forge loads them.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

Pack authors can select a named baseline in `config/thaumcraftupdater.toml`. With `installOnce = ["*"]`, the client checks for new members on every launch. Version 0.2.8.1 also installs missing third-party Forge mods directly from the URLs pinned by the pack author. Each file must match the published size, SHA-256 and Forge mod metadata. Existing third-party installations are kept.

The ATM9TC static extension sets `baseline = "atm9tc"` and `thirdPartyOnly = true`. Merge both its `config` and `mods` directories into the instance. Get the next static ZIP to change the bundled Thaumcraft series versions.

## Build

Install JDK 17 and point `JAVA_HOME` at the JDK. Run these commands from the repository root. The wrapper uses Gradle 8.8 and compiles against Forge 47.4.23.

Windows (PowerShell)

```powershell
.\gradlew.bat assemble
```

Linux / macOS

```sh
./gradlew assemble
```

The installable JAR is `build/libs/[TC4Continuation]ThaumcraftUpdater-<version>.jar`. The `-sources.jar` contains source code.

Gradle resolves the libraries from their Maven repositories. [Build dependencies](dependencies/build.gradle)
