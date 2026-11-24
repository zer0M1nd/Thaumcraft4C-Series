# 神秘时代 4：承续版

[English](README.md)

从扫描世界、认识要素开始，解锁研究、炼金、注魔、法杖与傀儡自动化，在 Minecraft 1.20.1 中重温神秘时代 4 的探索与魔法科技。

## 安装

适用于 Minecraft 1.20.1，最低要求为 Forge 47.4.0（47.x）。

必装模组为TerraBlender `[3.0.1.7,3.1)`、Curios API `[5.14.1,6)`。

可选联动包括 Just Enough Items（JEI）`[15.20.0,16)`、应用能源 2（AE2）、JECharacters、AgriCraft 和 Patchouli。 [完整的 Forge 依赖声明](src/main/resources/META-INF/mods.toml)

在 JEI 物品列表或 AE2 存储终端中，用 `!` 或 `！` 引出要素条件，与物品名或原生筛选词之间用空格分隔。`@minecraft !ordo` 和 `!ordo @minecraft` 都查找含秩序的原版物品。用空格分隔的多个要素条件需要同时满足。

`!1<=ordo<=5` 查找含 1 至 5 点秩序的物品，`!ordo&types=1` 查找只含秩序的物品，`!(aer|ignis)&-terra&sum>=4` 要求含风或火、不含地且总量至少为 4。支持 `=`、`<`、`>`、`<=`、`>=`。省略数量表示至少 1 点。可输入拉丁名、当前语言的译名，安装 JECharacters 后也可输入中文拼音。精确名称优先，否则按拉丁名前缀、译名或拼音子串匹配。

名称不区分大小写。先算 `-`（非），再算 `&`（与），最后算 `|`（或），英文括号可以改变顺序。表达式不完整或出现无法识别的要素名时，列表为空。

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
