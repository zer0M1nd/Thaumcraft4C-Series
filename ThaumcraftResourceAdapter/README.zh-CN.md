# ThaumcraftResourceAdapter

[English](README.md)

为神秘时代本体与移植扩展转换旧版材质，供 Minecraft 1.20.1 客户端在资源包菜单中选用。

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

可安装主包位于 `build/libs/[TC4Continuation]ThaumcraftResourceAdapter-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

Gradle 从 Maven 仓库解析所需库。[构建依赖](dependencies/build.gradle)
