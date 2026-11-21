# 来源与编辑记录

原始需求：`E:/AICoding/TC4Revival/Addons/ThaumicEnergistics/docs/RESOURCE_REMAKE_BRIEF.md`。制作过程按用户后续指示变更为指定基准图局部改色，并排除法杖核心、编码样板；最终驱动器外显统一纯紫。

| 类别 | 依据 | 本次处理 |
| --- | --- | --- |
| 普通元件 1k～256k、创造元件、空外壳、组件 1k～256k | 本机实际 `ae2-15.4.10.jar` 中 `assets/ae2/textures/item/` 对应 PNG | 局部调色；创造元件按用户要求整体下移 1px 对齐普通轮廓 |
| 高容量与特殊元件 | AE2 15.4.10 的 256k 元件结构 | 保留形状和分区，局部容量颜色与神秘锭外壳改色 |
| 高容量组件 | `.tooling/visual-redesign-reference/mega-cell_component_1m.png`、4m、16m；来源指向 [MEGACells 1.20](https://github.com/62832/MEGACells/tree/1.20/common/src/main/resources/assets/megacells/textures/item) | 保留框架轮廓，紫色核心色相对齐其他组件；名称仍用 1024k/4096k/16384k |
| 神秘锭外壳色阶 | 用户前序审阅的 `Output/0920_01/thaumiumingot.png` | 提取 8 级深紫金属色替换 AE2 外壳色，保留原图明暗位置 |
| 驱动器子模型 | 运行版 AE2 的 `block/drive/drive_cell` 几何与 UV 合约 | 新制纯紫色贴图，11 个文件内容相同；动态灯交给 AE2 |
| 终端、接口、装配室、齿轮箱、GUI | brief 的现行代码尺寸，现代 AE2 结构习惯，旧神秘能源 jar 的识别特征 | Aseprite 制作贴图，Blockbench MCP 制作几何、UV 和表面绘制 |

这是含上游衍生改色的资源，不将所有 PNG 声称为独立原创。AE2 上游见 [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)，MEGACells 上游见 [MEGACells](https://github.com/62832/MEGACells)。本包记录素材来源，不改写或取代上游的权利声明。

`source/references/` 仅供源文件重现和对照，**不在 runtime/ 内**；`source/scripts/recolor_provenance.json` 记录实际参考文件 SHA-256、偏移和允许修改的逐像素坐标。`validation.json` 验证参考 hash、透明轮廓和未允许区域的像素未变。AE2 驱动器框架只出现在标明为合成示意的预览中，没有复制进自有运行时纹理。

旧神秘能源参考 jar、旧材质、探索项目和被弃用的浅色试作没有作为本包运行时资产发布。Blockbench 截图中的原生展示参照人偶/展示框仅用于预览，不属于新 mod 模型。


# 0.2.6 资源反馈修正

- 输入仍为完整保留的 `art/0920_02-handoff`；本目录是独立修正层。
- 11 张硬盘外壳：用 Aseprite 将暗灯孔从图片 (4,3) 移至 (1,3)。AE2 朝北面的 UV 与世界 X 方向相反；实际灯珠由 AE2 根据状态绘制。
- 绿色显示层：通过 Aseprite 合并、着色本地 AE2 ME 合成终端的 dark/medium/bright 蒙版，保留原有九格形状和透明边缘；所有外框像素均透明，显示 AE2 原本灰框。
- 终端模型：在 Blockbench Java Block/Item 格式中保留 AE2 display_base 的面板、后接头几何及 UV，覆盖内部绿色显示层。世界模型直接继承 `ae2:part/display_base`，外框、侧面、背面资源由已安装的 AE2 提供。兼容 Minecraft 1.20.1。
- AE2 的模型构造及显示层衍生自 TeamAppliedEnergistics 的 Applied Energistics 2，对应许可 LGPL-3.0；不得将这部分误称为完全原创。参照 `references/AE2`，附 AE2 许可文件。
- GUI 下半区直接读取玩家提供的神秘能源 1.1.3.0 jar 内 `textures/gui/arcane.crafting.png`，交换按钮读取 `textures/gui/states.png`。发行包不包含这些旧版 PNG。
- 布局以实际旧版 jar 的 GuiConstants_ACT、ContainerPartArcaneCraftingTerminal、SlotArcaneCraftingResult 为准：基础高度 243，九宫格 (44,90)，法杖 (116,90)，结果 (116,126)，护甲 (8,81)，玩家物品栏 (8,162)、快捷栏 (8,220)。合成区随 ME 物品行数整体平移，每增加一行移动 18 像素。
- 重新生成：先运行 `scripts/resource_review_textures.lua`（Aseprite），再运行 `scripts/resource_review.py prepare`、`scripts/resource_review_models.py`（Blockbench MCP），最后运行 `scripts/resources.py`。PNG 和模型的哈希由本目录 manifest.json 明确列出；运行时资源通过该清单验证后安装。


# 0921_1 修订资源

- 三张物品图由用户提供的 `0921_01-three-item-textures.zip` 原样接入，来源与调色范围见 `handoff/README.md` 和原包 `validation.json`，不运行包内脚本。
- 无线奥术终端以 AE2 无线合成终端为基础，只修改紫色屏幕为绿色；充能卡保留 AE2 高级卡轮廓、电路，在中心加绿色魔力符号。使用 Aseprite 原生 Lua，附可编辑源。
- 装配室外圈采用 AE2 分子装配室的边框明暗纹路，改为暗紫色。16×16 纹理仅最外一圈 60 像素不透明；内部全部透明。六个面共用该纹理，原有内部几何与材质不变。
- 两种灵气接口仅更改 GUI 展示的 Y 旋转角 180 度。其世界模型和其他展示变换保持不变。
- AE2 派生部分遵循现有 LGPL-3.0 来源记录及随包许可；旧 TC4/TE 原始资源仍不内置。
- 中文研究副标题读取此前使用的本地 `ThaumicEnergistics-GTNH` 上游汉化源 `zh_CN.lang`，从原研究键映射至现代研究键，不使用标题代替副标题。
