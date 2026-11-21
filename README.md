# ThaumcraftResourceAdapter

[简体中文](README.zh-CN.md)

Convert local resource packs made for older Thaumcraft versions and addons into packs you can enable in Minecraft 1.20.1.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

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

The installable JAR is `build/libs/[TC4Continuation]ThaumcraftResourceAdapter-<version>.jar`. The `-sources.jar` contains source code.

Gradle resolves the libraries from their Maven repositories. [Build dependencies](dependencies/build.gradle)
