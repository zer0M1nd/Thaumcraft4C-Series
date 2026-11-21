package tc4revival.config;

import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.config.RevivalConfig;
import thaumcraft.api.config.RevivalProfile;

/** Independent additions; compatibility/aspect definitions are intentionally not listed here. */
public class CoreRevivalFeatures extends RevivalConfig {
    public final Option basicShortcuts, tableSorting, aspectSearch, autoRefill, randomResearch, relatedSearch, autoConnect;
    public final Option firstResearch, secondResearch, thirdResearch;
    public final Option bookSearch, bookSize, gogglesHeadSlot, armorToughness, expandedOres, underwaterNodes, dimensionNodes;
    public final Option repeatInfusion, labelSelection, orderedInfusion, enhancedEnchanting, randomizedNotes, inventoryScanning;
    public final Option batchFocusUpgrades, listeningGuidance, bookTutorials;

    public CoreRevivalFeatures(ForgeConfigSpec.Builder builder) {
        super(builder, false, () -> RevivalProfile.EVERYTHING);
        listeningGuidance = feature("research.listening_guidance", "聆听指引：解锁新研究后消耗要素获取一项缺失前置；JEI 隐藏其余未完成前置。DEFAULT 开启。", "Listening guidance: pay aspect points for one missing prerequisite; JEI hides other unfinished prerequisites. DEFAULT is enabled.", new Defaults(true,true,true,true));
        batchFocusUpgrades = feature("focal_manipulator.batch_upgrades", "一次选择多个核心升级，按先后顺序检查条件并支付总费用。", "Queue multiple focus upgrades; validate each in order and pay the combined cost.", Defaults.BENEFIT);
        basicShortcuts = feature("research_table.basic_shortcuts", "滚轮、数字提示和数字键放置；从不要求研究。", "Scroll wheel, number hints and number-key placement; never requires research.", Defaults.CONVENIENCE);
        tableSorting = feature("research_table.sorting", "要素排序和正反序切换，不附加研究门槛。", "Aspect sorting and direction controls; no research gate.", Defaults.BENEFIT);
        aspectSearch = feature("research_table.aspect_search", "普通要素搜索；启用时受第一阶段研究门槛控制。", "Normal aspect search; subject to the stage-one research gate when enabled.", Defaults.BENEFIT);
        autoRefill = feature("research_table.auto_refill", "自动学习已完成笔记并补充笔记和墨水；需第一阶段，且由玩家自行勾选。", "Learn completed notes and replenish notes/ink; stage one and the player's opt-in preference apply.", Defaults.BENEFIT);
        randomResearch = feature("research_table.random_research", "随机研究按钮；需第一阶段并消耗原有纸墨材料。", "Random research button; stage one and the original paper/ink costs apply.", Defaults.BENEFIT);
        relatedSearch = feature("research_table.related_search", "中键及 ~ 关联搜索，结果包含目标本身；需第二阶段。可独立提供关联查询。", "Middle-click and ~ related search, including the target itself; stage two. Can provide related queries independently.", Defaults.BENEFIT);
        autoConnect = feature("research_table.auto_connect", "中键拖动连线；需第三阶段，不替代合法性和资源检查。", "Middle-drag connection assistance; stage three, with normal legality/resource checks.", Defaults.BENEFIT);
        firstResearch = feature("research_table.research_gates.stage_1", "普通搜索、装填和随机研究是否要求第一阶段；全部关联功能关闭时不生效。", "Require stage one for search/refill/random research; inactive when all associated features are off.", Defaults.RESEARCH);
        secondResearch = feature("research_table.research_gates.stage_2", "关联搜索是否要求第二阶段；功能关闭时不生效，跳过禁用的新增前置。", "Require stage two for related search; inactive when the feature is off, omitting disabled added prerequisites.", Defaults.RESEARCH);
        thirdResearch = feature("research_table.research_gates.stage_3", "自动连线是否要求第三阶段；功能关闭时不生效，保留元始珍珠前置。", "Require stage three for auto-connect; inactive when the feature is off, retaining the Primordial Pearl prerequisite.", Defaults.RESEARCH);
        bookTutorials = feature("book.tutorials", "加载魔导手册的教程分页、本体与附属教程研究及其正文。关闭后保留已有教程进度。", "Load the Thaumonomicon tutorial tab, core and addon tutorial research, and their pages. Saved tutorial progress is preserved when disabled.", new Defaults(false, false, false, false));
        bookSearch = feature("book.search", "本体新增的魔导手册研究名称搜索和跳转；安装巫师宝具时，其原版自带搜索独立保留。", "Core-added Thaumonomicon name search and navigation; Witching Gadgets retains its original search independently when installed.", Defaults.BENEFIT);
        bookSize = feature("book.resizing", "手册尺寸切换；关闭时使用原有尺寸，保留玩家的尺寸偏好。", "Book size control; disabling uses the original size and preserves the player's preference.", Defaults.BENEFIT);
        gogglesHeadSlot = feature("equipment.goggles_head_slot", "揭示之护目镜可放入 Curios 头饰槽；不在该槽提供魔力减免。", "Allow revealing goggles in the Curios head slot, without its vis discount.", Defaults.BENEFIT);
        armorToughness = feature("equipment.armor_toughness", "新增护甲韧性加强；关闭时保留原版防御。", "Added armor toughness bonuses; disabling preserves original defense.", Defaults.BENEFIT);
        expandedOres = feature("world_generation.expanded_ores", "扩展矿石高度及按现代地层调整生成次数；不删除既有矿物。", "Expanded ore heights and attempts for modern strata; existing ores are preserved.", Defaults.BENEFIT);
        underwaterNodes = feature("world_generation.underwater_nodes", "海洋额外的水下自然节点生成尝试。", "Additional underwater natural-node attempt in oceans.", Defaults.BENEFIT);
        dimensionNodes = feature("world_generation.additional_dimensions", "在原版 TC4 自然节点维度以外生成节点；倍率由数值配置决定。", "Generate natural nodes outside the original TC4 dimensions; numerical multipliers apply.", Defaults.BENEFIT);
        repeatInfusion = feature("infusion.repeat_placement", "空手使用注魔矩阵重复放置上次配方材料。", "Use the infusion matrix with an empty hand to place the previous recipe's ingredients.", Defaults.BENEFIT);
        labelSelection = feature("alchemy.label_selection", "预先选择标签的要素。", "Select an aspect on a label before applying it.", Defaults.BENEFIT);
        orderedInfusion = feature("infusion.ordered_ingredients", "注魔材料必须按顺时针循环顺序摆放，允许整体旋转。", "Require clockwise cyclic ingredient order; rotation is allowed.", new Defaults(false, false, false, false));
        enhancedEnchanting = feature("infusion.enhanced_enchanting", "注魔附魔免经验、多级世界盐及虚空种子超等级加强。", "XP-free multilevel infusion enchanting and void-seed overlevel bonuses.", Defaults.BENEFIT);
        randomizedNotes = feature("research.randomized_notes", "随机化新研究笔记的初始要素顺序和角度；不改变已有笔记。", "Randomize new research-note seed order and angle; existing notes are unchanged.", Defaults.CHALLENGE);
        inventoryScanning = feature("scanning.inventory", "物品栏内扫描物品和玩家模型；不影响普通透镜扫描与副手适配。", "Inventory item/player-model scanning; normal scanning and offhand support remain available.", Defaults.BENEFIT);
    }

    public boolean stageEnabled(int stage) {
        return switch (stage) {
            case 1 -> firstResearch.get() && (aspectSearch.get() || autoRefill.get() || randomResearch.get());
            case 2 -> secondResearch.get() && relatedSearch.get();
            case 3 -> thirdResearch.get() && autoConnect.get();
            default -> false;
        };
    }
}
