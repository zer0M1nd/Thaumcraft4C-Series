# 心血结晶

[English](README.md)

复刻 The Heartwork 最终 FIX 版：钢材与军刀工具、料理和作物、命名刀与特殊技、拔刀注魔研究、能量刀，以及村庄职业与交易。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.23（47.x）。

必装模组为神秘时代 4：承续版 `0.4.116.2+`。

本体还需要 Curios API 和 TerraBlender。

可选联动包括SlashBlade: Resharped `[1.9.65,1.10)`、Industrial Craft 2 `[2.10.33-ex120,2.11)`、Botania `[1.20.1-456,1.20.2)`、exparmory。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

**首次启动需要手动提供心血结晶 FIX 原包。** 从原作下载页取得 1.7.10 FIX 版。出现资源提示时，把 `HeartWork_FIX_MC1.7.10.jar` 放进当前游戏实例的 `thaumcraft/` 文件夹，回到提示界面点**重试**。本体验证并提取资源后会清理导入的 JAR，可另外保存下载的原件。[原作下载页](https://www.mcmod.cn/download/1034.html)

## 构建

安装 JDK 17 和 Python 3.8 或更新版本，将 `JAVA_HOME` 指向该 JDK。在仓库根目录执行以下命令。Wrapper 使用 Gradle 8.8，编译目标为 Forge 47.4.23。

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

可安装主包位于 `build/libs/[TC4Continuation]The-Heartwork-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

开发运行需要额外联动宿主时，在准备命令末尾加 `--profile integrations`。

也可以使用从源码编出的系列主包。将本体（`thaumcraft`）的主包放入一个目录，使用 Python 3.11 或更高版本运行 `python3 dependencies/prepare.py --series-dir <目录>`（Windows 用 `py -3`），然后执行脚本输出的 Wrapper 命令。
