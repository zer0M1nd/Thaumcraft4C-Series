# 高等神秘学

[English](README.md)

复刻 Advanced Thaumaturgy AT2-30r：水银法杖与六种水晶升级、魔杖台、节点修改器、天域瓶、神秘肥料、源质引擎及微晶器。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.23（47.x）。

必装模组为神秘时代 4：承续版 `0.4.116.2+`。

本体还需要 Curios API 和 TerraBlender。

可选联动包括CC: Tweaked `1.116.1+`、OpenComputers `1.9.0+`。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

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

可安装主包位于 `build/libs/[TC4Continuation]Advanced-Thaumaturgy-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

也可以使用从源码编出的系列主包。将本体（`thaumcraft`）的主包放入一个目录，使用 Python 3.11 或更高版本运行 `python3 dependencies/prepare.py --series-dir <目录>`（Windows 用 `py -3`），然后执行脚本输出的 Wrapper 命令。
