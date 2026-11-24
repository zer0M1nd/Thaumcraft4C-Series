# 神秘万象

[English](README.md)

扩展神秘时代的实用魔法与现代模组联动：使用便捷源质设备、神秘调谐和魔法工具，并结合机械动力建造奥术飞艇与航线运输网络。

虚空砂纸可使用 64 次。服务器配置中的 `preserve_fake_player_void_tools` 控制机械手和代行石板使用虚空工具是否免耗耐久。`repair_deployer_tools` 控制法杖充能器是否修复机械手持有的修复附魔工具。两项默认关闭。

装有混沌矩阵的自动坩埚会显示机械动力序列装配的坩埚步骤所需的要素。要素齐全时，机器把半成品推进到下一步。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.0（47.x）。

必装模组为神秘时代 4：承续版 `4.3+`。

本体还需要 Curios API 和 TerraBlender。

可选联动包括Jade、Create `6.0.0+`、Botania、Tinkers’ Construct、JourneyMap、Productive Bees、神秘时代：未来 `0.2.10.2+`。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

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

可安装主包位于 `build/libs/[TC4Continuation]Thaumic-Omniverse-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

开发运行需要额外联动宿主时，在准备命令末尾加 `--profile integrations`。

构建 Create 0.5.1.j 兼容包时，在准备命令末尾加 `--profile create051`，再使用 Wrapper 执行 `assemble -Pcreate051`。兼容主包位于 `build-create051/libs/`，以 `-create051.jar` 结尾。

也可以使用从源码编出的系列主包。将 `thaumcraft`, `thaumicfutures` 各一个主包放入同一目录，使用 Python 3.11 或更高版本运行 `python3 dependencies/prepare.py --series-dir <目录>`（Windows 用 `py -3`），然后执行脚本输出的 Wrapper 命令。

完整回归测试还会读取本体开发仓中的研究和要素数据。
