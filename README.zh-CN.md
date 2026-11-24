<p align="center">
  <img src=".github/assets/logo-zh.png" alt="神秘时代 4：承续版" width="820">
</p>

# 神秘时代 4：承续版（TC4C）系列

[English](README.md) | **简体中文**

在 Minecraft 1.20.1 的 Forge 上重制神秘时代 4 及围绕它成长起来的附属。本仓库收录整个系列的公开源码：本体、25 个附属和两个独立工具，各占一个顶层文件夹。

## 项目声明

本项目为由 Azanor 创作的 Minecraft mod《神秘时代》（Thaumcraft）及其附属在 Minecraft 1.20.1 下的重制。
本项目的主要玩法框架基于 1.7.10 的神秘时代 4 及其生态。

作者与 Mojang AB、Azanor、Team CoFH 及其他扩展原始 mod 作者均无关联。

本项目不分发任何未经授权的原始资源文件。要正常使用本项目，需将旧版本对应 mod 置于游戏运行目录内以读取资源。

AI 使用声明：本项目使用生成式 AI 进行开发。本项目原创内容中含有使用生成式 AI 创作的资源。

## 下载与游玩

- **下载 mod、体验整合包**：[tc4c.zer0m1nd.me][site]
- **QQ 群**：1125142992
- **问题反馈与建议**：[GitHub Issues][issues]

## 开始使用

- 全系列面向 Minecraft 1.20.1、Forge 47.x 与 Java 17。各项目的 README 写明了各自的最低 Forge 版本。
- 本体需要 [TerraBlender](https://www.mcmod.cn/class/5489.html) 和 [Curios API](https://www.mcmod.cn/class/2029.html)。所有附属都需要本体，其余前置见下方表格。
- 原作的图片、音效和旧语言文件从游戏运行目录的 `thaumcraft` 文件夹读取。客户端首次启动时会检查该文件夹，尝试从原下载渠道获取仍缺少的旧版 mod 文件，校验后只提取需要的资源。自动获取失败时，同一界面会提示要下载哪个文件、放到哪里。旧版 JAR 只当作数据读取，不要放进 `mods`。独立服务端不需要这些文件。
- 两个独立工具不依赖本体。

## 项目列表

每个条目链接到对应文件夹，其中有各自的 README、许可声明和构建文件。

稳定性标记：

| | 含义 |
| :-: | --- |
| 🟢 | 经过多名玩家实际游玩测试，已经可获得较好的游戏体验。 |
| 🟡 | 经过一定程度的测试和少量实际游玩，可能存在 bug，但总体可玩。 |
| 🔴 | 开发/移植初步完成但尚未经广泛测试，现阶段不建议加入游玩。 |

“最新版本”列为已发布的最新版本号。三段为稳定版，四段为开发版。

### 本体

| | 项目 | 原作 | 内容 | 前置 | 最新版本 |
| :-: | --- | --- | --- | --- | --- |
| 🟢 | [**神秘时代 4：承续版**](TC4/README.zh-CN.md)<br>Thaumcraft 4: Continuation | Azanor 的 Thaumcraft 4 | 要素、研究、炼金、注魔、法杖与傀儡，系列的核心。 | TerraBlender、Curios API | <!--v:thaumcraft-->0.4.122 |

### 原创附属

为本系列新创作的附属。

| | 项目 | 内容 | 前置 | 最新版本 |
| :-: | --- | --- | --- | --- |
| 🟢 | [**神秘万象**](ThaumicOmniverse/README.zh-CN.md)<br>Thaumic Omniverse | 神秘调谐、便捷源质设备、现代模组联动与奥术飞艇。 | 本体；可选：机械动力 | <!--v:thaumicomniverse-->0.2.48 |

### 复刻附属

复刻旧版模组的附属。

| | 项目 | 原作 | 内容 | 前置 | 最新版本 |
| :-: | --- | --- | --- | --- | --- |
| 🟢 | [**神秘时代：未来**](ThaumicFutures/README.zh-CN.md)<br>Thaumic Futures | Azanor 的 Thaumcraft 5、6 | 带回神秘时代 5、6 的部分设备、工具与机制。 | 本体 | <!--v:thaumicfutures-->0.2.14 |
| 🟢 | [**神秘工匠**](ThaumicTinkerer/README.zh-CN.md)<br>Thaumic Tinkerer | Vazkii、pixlepix、nekosune 的 Thaumic Tinkerer | 实用工具、附魔与灵宝等后期装备。 | 本体 | <!--v:thaumic_tinkerer-->0.1.47 |
| 🟢 | [**自动化魔法**](Automagy/README.zh-CN.md)<br>Automagy | Tuhljin、Draanor 的 Automagy | 物品整理、红石控制与源质自动化。 | 本体 | <!--v:automagy-->0.1.23 |
| 🟢 | [**禁忌魔法**](ForbiddenMagic/README.zh-CN.md)<br>Forbidden Magic | SpitefulFox 的 Forbidden Magic | 七宗罪研究，禁忌法杖、工具与附魔。 | 本体；可选：血魔法、植物魔法 | <!--v:forbidden_magic-->0.1.22 |
| 🟢 | [**神秘能源**](ThaumicEnergistics/README.zh-CN.md)<br>Thaumic Energistics | Nividica 的 Thaumic Energistics | 把源质接入应用能源 ME 网络。 | 本体、应用能源 2 | <!--v:thaumicenergistics-->0.2.32 |
| 🟢 | [**神秘视界**](ThaumicHorizons/README.zh-CN.md)<br>Thaumic Horizons | Kentington 的 Thaumic Horizons | 生物改造、灵魂研究与特殊透镜。 | 本体 | <!--v:thaumichorizons-->0.1.29 |
| 🟢 | [**神秘基础学**](ThaumicBases/README.zh-CN.md)<br>Thaumic Bases | Modbder 的 Thaumic Bases | 作物、材料、工具与实用装置。 | 本体 | <!--v:thaumicbases-->0.1.29 |
| 🟡 | [**电力魔法工具**](ElectroMagicTools/README.zh-CN.md)<br>Electro-Magic Tools | fewizz、TheSilentium 的 Electro-Magic Tools | 电力驱动的魔法工具、装备与设备。 | 本体、工业时代 2 | <!--v:emt-->0.1.14 |
| 🟡 | [**魔像秘经**](Gadomancy/README.zh-CN.md)<br>Gadomancy | makeo、HellFirePvP 的 Gadomancy | 扩展傀儡、节点与注魔设备。 | 本体 | <!--v:gadomancy-->0.1.16 |
| 🟡 | [**节点力学**](NodalMechanics/README.zh-CN.md)<br>Nodal Mechanics | Shukaro 的 Nodal Mechanics | 构造和培育灵气节点。 | 本体 | <!--v:nodalmechanics-->0.1.7 |
| 🟡 | [**污秽魔法**](TaintedMagic/README.zh-CN.md)<br>Tainted Magic | John Yorke（tadashi_3）的 Tainted Magic | 污秽与扭曲研究，影金装备与法杖核心。 | 本体 | <!--v:taintedmagic-->0.1.14 |
| 🟡 | [**神秘注魔**](ThaumicInfusion/README.zh-CN.md)<br>Thaumic Infusion | DrunkMafia 的 Thaumic Infusion | 向方块注入要素，赋予魔法效果。 | 本体 | <!--v:thaumicinfusion-->0.1.11 |
| 🟡 | [**神秘扭曲学**](WarpTheory/README.zh-CN.md)<br>Warp Theory | Shukaro、thegreatunclean 的 Warp Theory | 更多扭曲事件与异常效果。 | 本体 | <!--v:warptheory-->0.1.9 |
| 🟡 | [**巫师宝具**](WitchingGadgets/README.zh-CN.md)<br>Witching Gadgets | BluSunrize 的 Witching Gadgets | 饰品、披风、工具与魔法设备。 | 本体 | <!--v:witchinggadgets-->0.1.26 |
| 🔴 | [**神秘议会**](ThaumicConcilium/README.zh-CN.md)<br>Thaumic Concilium | Ilya3000、BreadX22 的 Thaumic Concilium | 裂隙与虚体研究，法杖核心、法镯与新生物。 | 本体、禁忌魔法、神秘工匠 | <!--v:thaumicconcilium-->0.1.8 |
| 🔴 | [**神秘染料**](ThaumicDyes/README.zh-CN.md)<br>Thaumic Dyes | KryptonCaptain、Amerem 的 Thaumic Dyes | 神秘护甲染色、符文装备与面具。 | 本体 | <!--v:thaumicdyes-->0.1.7 |
| 🔴 | [**注魔酿造**](InfusionBrewing/README.zh-CN.md)<br>Infusion Brewing | mariri 的 Infusion Brewing | 流体药水、注魔强化与药水法杖核心。 | 本体 | <!--v:infusionbrewing-->0.1.4 |
| 🔴 | [**奥法献祭学**](ArcaneArteries/README.zh-CN.md)<br>Arcane Arteries | jordsta95、MaxIsH0t 的 Arcane Arteries | 以献祭刀具、石板与符文衔接血魔法。 | 本体、血魔法 | <!--v:arcanearteries-->0.1.4 |
| 🔴 | [**奥法工程学**](ArcaneEngineering/README.zh-CN.md)<br>Arcane Engineering | Eris Izalith（cadaverous_queen）的 Arcane Engineering | 可升级法杖、神秘钻头与推进靴。 | 本体、沉浸工程 | <!--v:arcane_engineering-->0.1.4 |
| 🔴 | [**高等神秘学**](AdvancedThaumaturgy/README.zh-CN.md)<br>Advanced Thaumaturgy | Lycaon 的 Advanced Thaumaturgy；MonzterAssassin、Nekosune、ToFam 的 Advanced Thaumaturgy 2 | 水银法杖、节点修改器与源质引擎。 | 本体 | <!--v:advancedthaumaturgy-->0.1.4 |
| 🔴 | [**失落遗物学**](ForgottenRelics/README.zh-CN.md)<br>Forgotten Relics | Integral（Aizistral）的 Forgotten Relics | 融合植物魔法的遗物、饰品与法术书。 | 本体、植物魔法 | <!--v:forgottenrelics-->0.1.4 |
| 🔴 | [**神秘探险**](ThaumicExploration/README.zh-CN.md)<br>Thaumic Exploration | Flaxbeard、gaeakat（nekosune）的 Thaumic Exploration | 冥想之缸、共享容器、杖柄杖端与魔法靴。 | 本体 | <!--v:thaumicexploration-->0.1.5 |
| 🔴 | [**量子魔法**](Technomancy/README.zh-CN.md)<br>Technomancy | Democretes、theflogat、Mordenkainen 的 Technomancy | 源质与能量机器、节点制造与仪式。 | 本体；可选：植物魔法、血魔法 | <!--v:technomancy-->0.1.4 |
| 🔴 | [**心血结晶**](Heartwork/README.zh-CN.md)<br>The Heartwork | Mysterious Mountain Smith 的 The Heartwork | 军刀、料理与作物、命名拔刀。 | 本体；可选：拔刀剑 Resharped | <!--v:heartwork-->0.1.3 |

### 独立工具

| | 项目 | 用途 | 最新版本 |
| :-: | --- | --- | --- |
| 🟢 | [**ThaumcraftUpdater**](ThaumcraftUpdater/README.zh-CN.md)<br>神秘时代更新器 | 启动时更新已安装的系列模组。不属于神秘时代附属。 | <!--v:thaumcraftupdater-->0.2.8 |
| 🟢 | [**ThaumcraftResourceAdapter**](ThaumcraftResourceAdapter/README.zh-CN.md)<br>资源包适配器 | 把旧版神秘时代资源包转换为 1.20.1 可用。仅客户端。 | <!--v:thaumcraftresourceadapter-->1.1.1 |

## 如何构建

每个项目都可以用自己文件夹里的 Gradle Wrapper 单独构建。所需工具、依赖准备步骤和具体命令见各个子仓库的 README。

## 参与维护与扩展开发

- 此项目主要由生成式 AI 进行开发。建议使用 [MCModding](https://github.com/zer0M1nd/MCModding) 配合 Agent 维护此项目。
- 欢迎基于此版本开发扩展。如果缺少某些扩展实现所需的 API，欢迎在 [Issues][issues] 提出。我们期待一个内部无 Mixin 的生态。
- 推荐仓库：[Thaumifier](https://github.com/zer0M1nd/Thaumifier) 可用于让 AI 写出魔导手册风格的文本。

## 许可

本系列以 TC4Revival Noncommercial License 1.0 发布，即 PolyForm Noncommercial License 1.0.0 加上关于转售与付费整合包的附加条件。详见本目录的 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)，以及各项目文件夹内的许可文件和声明。第三方材料沿用其自身条款。

[site]: http://tc4c.zer0m1nd.me
[issues]: https://github.com/zer0M1nd/Thaumcraft4C-Series/issues
