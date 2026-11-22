# 神秘时代 4：承续版

[English](README.md)

从扫描世界、认识要素开始，解锁研究、炼金、注魔、法杖与傀儡自动化，在 Minecraft 1.20.1 中重温神秘时代 4 的探索与魔法科技。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.0（47.x）。

必装模组为TerraBlender `[3.0.1.7,3.1)`、Curios API `[5.14.1,6)`。

可选联动包括Just Enough Items `[15.20.0,16)`、agricraft、patchouli。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

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

可安装主包位于 `build/libs/[TC4Continuation]Thaumcraft-<版本>.jar`。同目录的 `-sources.jar` 是源码包。

准备脚本按 SHA-256 校验每项输入。已有依赖可在准备命令末尾加 `--from-dir <文件夹>` 导入。[固定构建依赖](dependencies/artifacts.json)

完整回归测试还会读取神秘时代原包及旧研究基准数据。
