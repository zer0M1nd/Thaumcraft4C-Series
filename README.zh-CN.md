# 失落遗物学

[English](README.md)

探索失落研究，制作融合神秘时代与植物魔法的遗物、饰品和法术书。包含扫描领悟、注魔制作、灵气施法、传送与经验储存。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.23（47.x）。

必装模组为神秘时代 4：承续版 `0.4.116.2+`、Botania `1.20.1-445+`、Curios API `5.14.1+1.20.1+`。

本体还需要 Curios API 和 TerraBlender。

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

可安装主包位于 `build/libs/[TC4Continuation]Forgotten-Relics-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

也可以使用从源码编出的系列主包。将本体（`thaumcraft`）的主包放入一个目录，使用 Python 3.11 或更高版本运行 `python3 dependencies/prepare.py --series-dir <目录>`（Windows 用 `py -3`），然后执行脚本输出的 Wrapper 命令。
