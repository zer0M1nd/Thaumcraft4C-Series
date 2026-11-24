# 巫师宝具

[English](README.md)

加入实用饰品、披风、工具和魔法设备，扩展旅行、资源处理与节点相关玩法。

扩展金属粒嬗变跟随本体的金属要素费用设置，另保留各种金属所需的其他要素。金属锭沿用现有的合成和熔炼途径。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.13（47.x）。

必装模组为神秘时代 4：承续版 `4.3+`、Curios API `5+`。

本体还需要 Curios API 和 TerraBlender。

可选联动包括Botania、CB Multipart、Tinkers’ Construct、Rack It Up、The Twilight Forest、Just Enough Items。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

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

可安装主包位于 `build/libs/[TC4Continuation]Witching-Gadgets-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

开发运行需要额外联动宿主时，在准备命令末尾加 `--profile integrations`。

也可以使用从源码编出的系列主包。将本体（`thaumcraft`）的主包放入一个目录，使用 Python 3.11 或更高版本运行 `python3 dependencies/prepare.py --series-dir <目录>`（Windows 用 `py -3`），然后执行脚本输出的 Wrapper 命令。
