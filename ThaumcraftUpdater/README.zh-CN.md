# ThaumcraftUpdater

[English](README.md)

在 Forge 扫描模组前，将已安装的神秘时代 4 系列模组更新到最新版本。独立启动工具，不属于神秘时代附属。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.0（47.x）。

## 构建

安装 JDK 17，将 `JAVA_HOME` 指向该 JDK。在仓库根目录执行以下命令。Wrapper 使用 Gradle 8.8，编译目标为 Forge 47.4.23。

Windows (PowerShell)

```powershell
.\gradlew.bat assemble
```

Linux / macOS

```sh
./gradlew assemble
```

可安装主包位于 `build/libs/[TC4Continuation]ThaumcraftUpdater-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

Gradle 从 Maven 仓库解析所需库。[构建依赖](dependencies/build.gradle)
