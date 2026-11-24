# ThaumcraftUpdater

[English](README.md)

在 Forge 扫描模组前，将已安装的神秘时代 4 系列模组更新到最新版本。独立启动工具，不属于神秘时代附属。

稳定版支持两段和三段版本号。更新器将 4.3 与 4.3.0 视为同一个版本。4.3.0.1 等四段开发版本排在两者之后。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.0（47.x）。

整合包可在 `config/thaumcraftupdater.toml` 中指定命名基线，并用 `installOnce = ["*"]` 在每次启动时补装新增成员。0.2.8.1 起也支持从维护者指定的上游地址补装第三方 Forge Mod。下载须通过文件大小、SHA-256、Mod 身份和版本校验。玩家已安装的第三方 Mod 会沿用现有版本。

ATM9TC 静态补充包已配置 `baseline = "atm9tc"` 和 `thirdPartyOnly = true`。安装时将包内的 `config` 和 `mods` 一起合并到游戏目录。更新器只补装第三方 Mod，系列 Mod 随下一份静态补充包更新。

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
