package tc4revival.config;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.*;

/** Gameplay settings belong to the loaded world; visual preferences stay on the client. */
public  class TCConfig {
    public static final Server SERVER;
    public static final Client CLIENT;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        var server = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
        SERVER.revival.bind(SERVER_SPEC);
        var client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
        CLIENT.revival.bind(CLIENT_SPEC);
    }

    private TCConfig() {}

    public static void register(ModLoadingContext context) {
        var serverMigration = new MigratingConfigSpec(SERVER_SPEC)
                .booleanOption("ordered_infusion", SERVER.orderedInfusion.path())
                .booleanOption("enhanced_infusion_enchanting", SERVER.enhancedInfusionEnchanting.path())
                .booleanOption("randomized_research_notes", SERVER.randomizedResearchNotes.path());
        for (var option : SERVER.branchFeatures.options()) serverMigration.booleanOption("branch_features", option.path());
        context.registerConfig(ModConfig.Type.SERVER, serverMigration, "thaumcraft-server.toml");
        var clientMigration = new MigratingConfigSpec(CLIENT_SPEC)
                .booleanOption("visuals.maximum_research_browser", CLIENT.maximumResearchBrowser.path())
                .booleanOption("visuals.aspect_icon_tooltips", CLIENT.aspectIconTooltips.path())
                .booleanOption("research_table.shortcuts", CLIENT.researchTableShortcuts.path())
                .booleanOption("research_table.auto_connect", CLIENT.researchTableAutoConnect.path())
                .booleanOption("visuals.small_book_font", CLIENT.smallBookFont.path())
                .map("research_table.search_strategy", "revival_features.research_table.search_strategy", value -> value instanceof Number n ? switch(n.intValue()) { case 0 -> "ORIGINAL"; case 1 -> "TRANSLATED"; case 2 -> "BOTH"; default -> "DEFAULT"; } : "DEFAULT")
                .move("visuals.research_obfuscation", "revival_features.book.research_obfuscation");
        context.registerConfig(ModConfig.Type.CLIENT, clientMigration, "thaumcraft-client.toml");
    }

    public enum WorldFeature {
        AURA_NODES("aura_nodes"), STRUCTURES("structures"), CINNABAR_ORE("cinnibar_ore"),
        AMBER_ORE("amber_ore"), INFUSED_STONE("infused_stone"), TREES("trees"), TAINT("taint");

        private final String configName;

        WorldFeature(String configName) { this.configName = configName; }
    }

    public enum SpawnKind {
        ANGRY_ZOMBIES, FIRE_BATS, WISPS, TAINTACLES, TAINT_SPORES, PECHS, ELDERCREATURES
    }

    public enum ResearchObfuscation { DEFAULT, NEVER, ALWAYS, DISTANCE }
    public enum LegacyTextMode { HIDDEN, HIGHLIGHT, ALL }
    public enum SearchStrategy { DEFAULT, ORIGINAL, TRANSLATED, BOTH }

    public static  class Server {
        private final EnumMap<WorldFeature, BooleanValue> generation = new EnumMap<>(WorldFeature.class);
        private final EnumMap<WorldFeature, BooleanValue> regeneration = new EnumMap<>(WorldFeature.class);
        private final EnumMap<SpawnKind, BooleanValue> spawning = new EnumMap<>(SpawnKind.class);
        public final ForgeConfigSpec.ConfigValue<String> regenerationKey;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> portableHoleBlacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> nodeDimensionMultipliers;
        private record NodeRulesSnapshot(List<? extends String> source,NodeDimensionRules rules) {}
        private volatile NodeRulesSnapshot nodeRulesSnapshot;
        public final IntValue researchDifficulty, aspectPoolSoftCap, nodeRarity;
        public final IntValue taintBiomeWeight, magicalForestBiomeWeight, taintSpreadRate;
        public final ForgeConfigSpec.DoubleValue infusedStoneDensityMultiplier;
        private final IntValue specialNodeRarity, golemDelay, golemIgnoreDelay;
        public final ForgeConfigSpec.DoubleValue golemSpeedMultiplier, golemCarryMultiplier, golemInteractionBonus;
        private final IntValue shieldRecharge, shieldWait, shieldCost;
        public final CoreRevivalFeatures revival;
        @Deprecated public final RevivalConfig.LegacyGroup branchFeatures;
        public final RevivalConfig.Option orderedInfusion, enhancedInfusionEnchanting, randomizedResearchNotes;
        public final BooleanValue advancedAspectInference;
        public final IntValue inferredAspectAmountLimit, inferredAspectTypeLimit;
        public final BooleanValue inferredAspectScaling;
        public final IntValue inferredAspectSoftLimit, inferredAspectHardLimit, inferredAspectBeforeScaling;
        public final BooleanValue championMobs, allowMirrors, allowCheatSheet, wardedStone;
        public final BooleanValue hardNodes, disableWarp, taintFromFlux, golemChestInteract;

        private Server(ForgeConfigSpec.Builder builder) {
            advancedAspectInference=builder.comment("进阶要素推断 / Advanced aspect inference. Fill missing aspects using recipes and configurable fallbacks. Requires JEI on the server. Restart the world/server after changing.").worldRestart().comment("根据配方和回退规则补全缺失要素；属于兼容适配，不受全局模式关闭","Supplement missing aspects from recipes and fallback rules; independent of the revival profile","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("advanced_aspect_inference",true);
            inferredAspectAmountLimit=builder.comment("自动推断的单种要素数值上限 / Maximum amount per automatically inferred aspect. Explicit definitions are unchanged. Restart the world/server.").worldRestart().comment("自动推断每种要素的数量上限，不改变显式要素","Maximum quantity of each inferred aspect; explicit entries are unchanged","初始值 / Default: " + String.valueOf(64) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(10000), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("inferred_aspect_amount_limit",64,1,10000);
            inferredAspectTypeLimit=builder.comment("自动推断的要素种类上限 / Maximum aspect types per automatically inferred entry. Explicit definitions are unchanged. Restart the world/server.").worldRestart().comment("每个推断条目的要素种类上限","Maximum aspect types in an inferred entry","初始值 / Default: " + String.valueOf(10) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(1024), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("inferred_aspect_type_limit",10,1,1024);
            inferredAspectScaling=builder.comment("要素推断数量指数缩放：开启时忽略 amount_limit / Expand inputs with g, sum and apply recipe factors, then compress with f. Restart world/server.").worldRestart().comment("启用要素指数缩放；启用后忽略单种数量上限","Enable exponential aspect scaling, superseding the simple amount limit","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("inferred_aspect_exponential_scaling",true);
            builder.push("inferred_aspect_scaling");
            inferredAspectSoftLimit=builder.comment("Linear threshold; must be smaller than hard_limit and hard_limit_before_scaling.").worldRestart().comment("线性缩放阈值；须小于两个硬上限","Linear scaling threshold; must be below both hard limits","初始值 / Default: " + String.valueOf(32) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(9999), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("soft_limit",32,1,9999);
            inferredAspectHardLimit=builder.comment("Maximum scaled quantity; must exceed soft_limit.").worldRestart().comment("压缩后的数量上限；须大于软阈值","Maximum compressed quantity; must exceed the soft threshold","初始值 / Default: " + String.valueOf(256) + "; 范围 / Range: " + String.valueOf(2) + ".." + String.valueOf(10000), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("hard_limit",256,2,10000);
            inferredAspectBeforeScaling=builder.comment("Maximum expanded quantity; must exceed soft_limit.").worldRestart().comment("展开后的数量上限；须大于软阈值","Maximum expanded quantity; must exceed the soft threshold","初始值 / Default: " + String.valueOf(1048576) + "; 范围 / Range: " + String.valueOf(2) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("hard_limit_before_scaling",1048576,2,Integer.MAX_VALUE);
            builder.pop();
            revival = new CoreRevivalFeatures(builder);
            branchFeatures = new RevivalConfig.LegacyGroup(revival.tableSorting, revival.aspectSearch, revival.autoRefill,
                    revival.randomResearch, revival.bookSearch, revival.gogglesHeadSlot, revival.armorToughness,
                    revival.expandedOres, revival.underwaterNodes, revival.repeatInfusion, revival.labelSelection);
            orderedInfusion = revival.orderedInfusion;
            enhancedInfusionEnchanting = revival.enhancedEnchanting;
            randomizedResearchNotes = revival.randomizedNotes;
            builder.push("world_generation");
            for (var feature : WorldFeature.values()) {
                generation.put(feature, builder.comment("原作世界生成开关 / Original world generation toggle: "+feature.configName,"初始 true；只作用于新生成区块 / Initial true; affects newly generated chunks.").define("generate_" + feature.configName, true));
            }
            infusedStoneDensityMultiplier=builder.comment("Multiplier for all six infused-stone vein attempt counts, without changing height distribution, vein size or element selection. 1 keeps default density, 0 disables generation. Independent of revival feature profiles. Only affects newly generated chunks or explicitly enabled ore retrogen. Restart the world/server after changing.")
                    .worldRestart().comment("六种蕴魔石的统一生成次数倍率；1为原密度，0禁止，不改变高度和矿脉大小；还需开启原有蕴魔石生成或补生成","Shared attempt multiplier for all six infused stones: 1 original density, 0 disabled. Requires the legacy generation or retrogen option; does not change heights or vein sizes","初始值 / Default: " + String.valueOf(1.0) + "; 范围 / Range: " + String.valueOf(0.0) + ".." + String.valueOf(100.0), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("infused_stone_density_multiplier",1.0,0.0,100.0);
            nodeRarity = builder.comment("Average chunks per wild node. Oceans also roll an extra underwater node at this same rarity when revival_features.world_generation.underwater_nodes is enabled; at most one natural node per chunk. Underwater nodes are a Modern Port addition. Zero or negative values would crash old generation.")
                    .comment("每个自然节点平均对应的区块数；额外水下尝试须启用 underwater_nodes，每区块最多一个自然节点","Average chunks per natural node; extra underwater attempts require underwater_nodes, with at most one natural node per chunk","初始值 / Default: " + String.valueOf(36) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("node_rarity", 36, 1, Integer.MAX_VALUE);
            nodeDimensionMultipliers=builder.comment("逐维度灵气节点倍率。所有未列出的维度默认为 1；0 关闭该维度自然节点。暮色森林默认 2。格式 namespace:dimension=multiplier。只影响新生成区块或明确开启的节点补生成，不移除已有节点。",
                    "Wild aura nodes in every dimension: unspecified = 1, zero = disabled. Chance is capped at one node per chunk; ocean surface/underwater rolls keep their existing rules.")
                    .comment("维度ID=倍率，未列出为1，0关闭，暮色森林默认2，Ad Astra 星球及轨道默认0；额外维度仍需开启 additional_dimensions","Dimension ID=multiplier; unspecified is 1, 0 disables, Twilight Forest defaults to 2; Ad Astra planets and orbits default to 0. Additional dimensions still require additional_dimensions","初始值 / Default: " + String.valueOf(NodeDimensionRules.DEFAULTS), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineListAllowEmpty(List.of("node_dimension_multipliers"),()->NodeDimensionRules.DEFAULTS,NodeDimensionRules::valid);
            specialNodeRarity = integer(builder, "special_node_rarity", 18, "One special node in N; effective minimum is 3.");
            taintBiomeWeight = builder.comment("TerraBlender forest-climate region weight (not an area percentage). Zero disables natural generation. Values above 10000 are capped. Restart the world/server to apply.").worldRestart().comment("腐化之地的森林气候区域权重，不是面积百分比；0关闭，上限10000","Tainted Lands forest-climate region weight, not area percentage; 0 disables, capped at 10000","初始值 / Default: " + String.valueOf(2) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("taint_biome_weight", 2, 0, Integer.MAX_VALUE);
            magicalForestBiomeWeight = builder.comment("TerraBlender forest-climate region weight (not an area percentage). Zero disables natural generation. Values above 10000 are capped. Restart the world/server to apply.").worldRestart().comment("魔法森林的森林气候区域权重，不是面积百分比；0关闭，上限10000","Magical Forest forest-climate region weight, not area percentage; 0 disables, capped at 10000","初始值 / Default: " + String.valueOf(5) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("magical_forest_biome_weight", 5, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.comment("对缺少当前补生成标识的已有区块应用选定的原有生成项目。", "Apply selected old features to chunks lacking the current regeneration key.")
                    .push("world_regeneration");
            regenerationKey = builder.comment("Changing this key makes selected regeneration eligible again.")
                    .comment("区块补生成标识；修改后允许所选项目再次补生成","Chunk retrogen marker; changing it makes selected features eligible again","初始值 / Default: " + String.valueOf("DEFAULT"), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("regen_key", "DEFAULT");
            for (var feature : WorldFeature.values()) {
                regeneration.put(feature, builder.comment("原作区块补生成 / Original chunk retrogen: "+feature.configName,"初始 false，可选 true/false；对缺少当前 regen_key 的已有区块生效，重启世界 / Default false, true/false; applies to existing chunks lacking the current regen_key; restart the world.").define(feature.configName, false));
            }
            builder.pop();

            builder.push("research");
            researchDifficulty = builder.comment("-1: purchase research; 0: normal; 1: solve all research at the table.")
                    .comment("-1购买研究，0标准，1全部研究需研究台笔记","-1 purchases research, 0 normal, 1 requires table notes for all research","初始值 / Default: " + String.valueOf(0) + "; 范围 / Range: " + String.valueOf(-1) + ".." + String.valueOf(1), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("research_difficulty", 0, -1, 1);
            aspectPoolSoftCap = integer(builder, "aspect_total_cap", 100, "Research pool threshold for reduced scanning gains; not a storage limit.");
            builder.pop();

            builder.push("monster_spawning");
            for (var kind : SpawnKind.values()) {
                spawning.put(kind, builder.comment("生物自然生成 / Natural spawning: "+kind.name(),"初始 true；关闭不清除已有实体 / Initial true; disabling preserves existing entities.").define("spawn_" + kind.name().toLowerCase(java.util.Locale.ROOT), true));
            }
            championMobs = builder.comment("Disabling normal champions still allows the old rare dangerous-location cases.")
                    .comment("启用普通精英怪；关闭仍保留原作危险地点的罕见特例","Enable ordinary champions; original rare dangerous-location exceptions remain","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("champion_mobs", true);
            builder.pop();

            builder.push("general");
            allowMirrors = builder.comment("允许魔镜相关玩法与研究","Enable magic mirror gameplay and research","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("allow_mirrors", true);
            allowCheatSheet = builder.comment("允许作弊魔导手册","Allow the cheat-sheet Thaumonomicon","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("allow_cheat_sheet", false);
            wardedStone = builder.comment("Enable protective properties of warded blocks, doors and glass.")
                    .comment("守卫方块、门和玻璃的保护特性","Protective properties of warded blocks, doors and glass","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("allow_warded_stone", true);
            hardNodes = builder.comment("原作困难节点规则","Original hard-mode node rules","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("hard_mode_nodes", true);
            disableWarp = builder.comment("关闭扭曲负面效果","Disable harmful warp effects","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("wuss_mode", false);
            taintFromFlux = builder.comment("允许咒波引发腐化生态群系","Allow flux to create taint biomes","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("biome_taint_from_flux", true);
            golemChestInteract = builder.comment("Let golems trigger container opening and closing events.")
                    .comment("傀儡访问容器时触发开启和关闭事件","Let golems trigger container opening and closing events","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("golem_chest_interact", true);
            taintSpreadRate = builder.comment("One spreading attempt in N block updates; zero disables spreading.")
                    .comment("每N次方块更新尝试腐化蔓延；0禁止","Attempt taint spreading once per N block updates; 0 disables","初始值 / Default: " + String.valueOf(200) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("biome_taint_spread", 200, 0, Integer.MAX_VALUE);
            golemDelay = integer(builder, "golem_delay", 5, "Task check interval in ticks; effective minimum is 1.");
            golemSpeedMultiplier = builder.comment("傀儡移动速度乘数 / Golem movement speed multiplier. Default: 1.5; restart world.").defineInRange("golem_speed_multiplier", 1.5, 0.05, 16.0);
            golemCarryMultiplier = builder.comment("傀儡运载量乘数 / Golem cargo capacity multiplier. Default: 2; restart world.").defineInRange("golem_carry_multiplier", 2.0, 0.05, 64.0);
            golemInteractionBonus = builder.comment("傀儡交互距离加成（格） / Additional golem interaction reach in blocks. Default: 1; restart world.").defineInRange("golem_interaction_bonus", 1.0, 0.0, 16.0);
            golemIgnoreDelay = integer(builder, "golem_ignore_delay", 10000, "Failed destination timeout in milliseconds; effective minimum is 1000.");
            portableHoleBlacklist = builder.comment("携带式洞穴不能穿透的方块 ID 列表；初始 [minecraft:iron_door]，允许空列表；重启世界。", "Namespaced block IDs through which the portable hole cannot pass; default [minecraft:iron_door], empty is allowed; restart the world.")
                    .defineListAllowEmpty(List.of("portablehole_blacklist"), () -> List.of("minecraft:iron_door"),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
            builder.pop();

            builder.push("runic_shielding");
            shieldRecharge = integer(builder, "runic_recharge_speed", 2000, "Milliseconds between recharge ticks; effective minimum is 500.");
            shieldWait = integer(builder, "runic_recharge_delay", 80, "Ticks after a shield breaks before recharge; effective minimum is 0.");
            shieldCost = integer(builder, "runic_cost", 50, "Centi-vis of both aer and terra per shield point; effective minimum is 0.");
            builder.pop();
        }

        public boolean generates(WorldFeature feature) { return generation.get(feature).get(); }
        public double nodeMultiplier(ResourceLocation dimension) {
            var entries=nodeDimensionMultipliers.get();var snapshot=nodeRulesSnapshot;
            if(snapshot==null||snapshot.source()!=entries) {
                snapshot=new NodeRulesSnapshot(entries,new NodeDimensionRules(entries));nodeRulesSnapshot=snapshot;
            }
            return snapshot.rules().multiplier(dimension.toString());
        }
        public boolean regenerates(WorldFeature feature) { return regeneration.get(feature).get(); }
        public boolean spawns(SpawnKind kind) { return spawning.get(kind).get(); }
        public int specialNodeRarity() { return Math.max(3, specialNodeRarity.get()); }
        public int golemDelayTicks() { return Math.max(1, golemDelay.get()); }
        public int golemIgnoreMilliseconds() { return Math.max(1000, golemIgnoreDelay.get()); }
        public int shieldRechargeMilliseconds() { return Math.max(500, shieldRecharge.get()); }
        public int shieldWaitTicks() { return Math.max(0, shieldWait.get()); }
        public int shieldCostCentiVis() { return Math.max(0, shieldCost.get()); }
    }

    public static  class Client {
        public final BooleanValue colorBlind, shaders, crookedLabels, wandDialBottom, displayAspects;
        public final BooleanValue blueMagicalForest, glowingTaint;
        public final BooleanValue nativeResearchTooltips;
        public final RevivalConfig.Option smallBookFont;
        public final RevivalConfig revival;
        public final RevivalConfig.Option aspectIconTooltips;
        public final RevivalConfig.Option researchTableShortcuts, researchTableAutoConnect;
        public final RevivalConfig.Option maximumResearchBrowser;
        public final ForgeConfigSpec.EnumValue<LegacyTextMode> legacyTextMode;
        public final RevivalConfig.Option jeiResearch;
        public final ForgeConfigSpec.EnumValue<SearchStrategy> researchTableSearchStrategy;
        public final IntValue researchTableColumn;
        public final ForgeConfigSpec.EnumValue<ResearchObfuscation> researchObfuscation;
        public final IntValue researchObfuscationDistance;
        public final IntValue notificationDelay, notificationMax;
        private final IntValue golemLinkQuality;

        public final ForgeConfigSpec.ConfigValue<String> chineseLocalization;

        private Client(ForgeConfigSpec.Builder builder) {
            chineseLocalization = builder.comment("下载中文本地化资源。可填英文或中文。ENABLED / 启用，DISABLED / 禁用，DETECT / 检测（默认，检查网络出口是否在中国大陆）。",
                    "Download Chinese localization resources. DETECT enables downloads only on a mainland China network.")
                    .define(tc4revival.download.DownloadPreferences.KEY, "DETECT", value -> tc4revival.download.DownloadPreferences.parse(value).isPresent());
            revival = new RevivalConfig(builder, () -> SERVER.revival.profile());
            jeiResearch = revival.feature("jei.research_queries", "JEI 研究查询、前置页面与魔导手册快捷查询。DEFAULT 跟随聆听指引开关；显式 TRUE/FALSE 优先；需要安装 JEI。", "JEI research queries and book shortcuts. DEFAULT follows listening guidance; explicit TRUE/FALSE takes priority; requires JEI.", () -> SERVER.revival.listeningGuidance.get());
            maximumResearchBrowser = revival.feature("book.maximum_research_browser", "最大化手册视图；游戏内尺寸按钮保存显式偏好。", "Maximize the book; the in-game size button saves an explicit preference.", new RevivalConfig.Defaults(false, false, false, false));
            aspectIconTooltips = revival.feature("tooltips.aspect_icons", "以图标显示要素提示；Shift 切回原有文字。", "Aspect icon tooltips; Shift restores the original text.", RevivalConfig.Defaults.BENEFIT);
            smallBookFont = revival.feature("book.small_font", "使用原有半尺寸研究副标题；关闭为清晰的大字体。", "Use original half-size research subtitles; disabling improves readability with larger text.", new RevivalConfig.Defaults(false,false,true,true));
            var localPreference = new RevivalConfig.Defaults(true, true, true, true);
            researchTableShortcuts = revival.feature("research_table.shortcuts", "允许基础快捷操作；仍受服务端对应功能配置控制。", "Allow basic shortcuts; the corresponding server feature must also be enabled.", localPreference);
            researchTableAutoConnect = revival.feature("research_table.auto_connect", "允许自动连线；仍受服务端功能及研究门槛控制。", "Allow auto-connect; server feature and research requirements still apply.", localPreference);
            legacyTextMode = builder.comment("手册旧版文本变更显示方式。","Display of revised legacy book text: HIDDEN, HIGHLIGHT, ALL.")
                    .defineEnum("revival_features.book.legacy_text_changes", LegacyTextMode.HIDDEN);
            researchObfuscation=builder.comment("研究标题混淆：DEFAULT 在 Everything/Easiest 为 NEVER，在 Hardest/Vanilla 为 ALWAYS；DISTANCE 使用距离阈值。", "Title obfuscation: DEFAULT is NEVER in Everything/Easiest, ALWAYS in Hardest/Vanilla; DISTANCE uses the threshold.", "初始 DEFAULT；可选 DEFAULT/NEVER/DISTANCE/ALWAYS；重开界面生效 / Default DEFAULT; allowed DEFAULT/NEVER/DISTANCE/ALWAYS; reopen the screen.").defineEnum("revival_features.book.research_obfuscation",ResearchObfuscation.DEFAULT);
            builder.push("visuals");
            nativeResearchTooltips = builder.comment("研究图标和分页使用原版物品样式提示框；默认开启，关闭后研究图标恢复旧样式。",
                    "Use vanilla item-style tooltips for research icons and category tabs. Default true; false restores legacy research icon tooltips.",
                    "可选 true/false；客户端配置重载后生效 / Allowed true/false; applies after client config reload.")
                    .define("native_research_tooltips", true);
            researchObfuscationDistance=builder.comment("Research grid distance to the closest available research in the same category.").comment("到同分类最近可解锁研究的网格距离；仅DISTANCE模式使用","Grid distance to the nearest available research in the category; only used by DISTANCE","初始值 / Default: " + String.valueOf(4) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(1000), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("research_obfuscation_distance",4,0,1000);
            colorBlind = builder.comment("原作色盲辅助显示","Original color-blind assistance","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("color_blind", false);
            shaders = builder.comment("启用神秘时代着色效果","Enable Thaumcraft shader effects","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("shaders", true);
            crookedLabels = builder.comment("允许标签略微倾斜","Allow crooked labels","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("crooked", true);
            wandDialBottom = builder.comment("魔杖仪表显示在屏幕下方","Display the wand dial at the bottom","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("wand_dial_bottom", false);
            displayAspects = builder.comment("Show aspects normally and hide them while holding Shift.")
                    .comment("平时显示要素，按住Shift时隐藏","Show aspects normally and hide them while holding Shift","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("display_aspects", false);
            blueMagicalForest = builder.comment("魔法森林使用蓝色色调","Use blue Magical Forest colors","初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("blue_magical_forest", false);
            glowingTaint = builder.comment("启用腐化发光效果","Enable glowing taint effects","初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").define("glowing_taint", true);
            notificationDelay = integer(builder, "notification_delay", 5000, "Notification scrolling timing, in milliseconds.");
            notificationMax = integer(builder, "notification_max", 15, "Maximum simultaneous notifications.");
            golemLinkQuality = integer(builder, "golem_link_quality", 16, "Values below 4 disable marker connection effects.");
            builder.pop();
            researchTableSearchStrategy = builder.comment("要素搜索策略：DEFAULT 在 Everything/Easiest 搜索原名和译名，在 Hardest/Vanilla 仅原名；仍需对应搜索权限。初始 DEFAULT。",
                    "Aspect search: DEFAULT searches both names in Everything/Easiest and original names in Hardest/Vanilla. Search permission is still required. Initial: DEFAULT.",
                    "ORIGINAL 原名；TRANSLATED 译名；BOTH 两者 / ORIGINAL, TRANSLATED or BOTH; reopen the table after editing / 修改后重开研究台。")
                    .defineEnum("revival_features.research_table.search_strategy", SearchStrategy.DEFAULT);
            builder.push("research_table");
            researchTableColumn = builder.comment("保存未过滤要素列表的操作列，界面自动校正越界值","Remember the active column in the unfiltered aspect list; the screen clamps out-of-range values","初始值 / Default: " + String.valueOf(0) + "; 范围 / Range: " + String.valueOf(0) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界/服务器；客户端项重开界面 / Restart world/server; reopen the screen for client settings.").defineInRange("unfiltered_column", 0, 0, Integer.MAX_VALUE);
            builder.pop();
        }

        public int searchStrategy() {
            var selected = researchTableSearchStrategy.get();
            if(selected != SearchStrategy.DEFAULT) return selected.ordinal()-1;
            return switch(SERVER.revival.profile()) { case HARDEST, VANILLA -> 0; default -> 2; };
        }

        public ResearchObfuscation obfuscation() {
            var value = researchObfuscation.get();
            if (value != ResearchObfuscation.DEFAULT) return value;
            return switch (SERVER.revival.profile()) {
                case HARDEST, VANILLA -> ResearchObfuscation.ALWAYS;
                default -> ResearchObfuscation.NEVER;
            };
        }
        public int golemLinkQuality() { return golemLinkQuality.get() < 4 ? 0 : golemLinkQuality.get(); }
    }

    private static IntValue integer(ForgeConfigSpec.Builder builder, String name, int defaultValue, String description) {
        String zh = switch(name) {
            case "special_node_rarity" -> "特殊节点稀有度，每N个节点一个，生效最小值3";
            case "aspect_total_cap" -> "研究点软阈值，超过后扫描收益下降，并非存储上限";
            case "golem_delay" -> "傀儡任务检查间隔，单位tick，生效最小值1";
            case "golem_ignore_delay" -> "傀儡忽略失败目标的毫秒数，生效最小值1000";
            case "runic_recharge_speed" -> "符文护盾补充间隔毫秒数，生效最小值500";
            case "runic_recharge_delay" -> "护盾破裂后的补充等待tick数，生效最小值0";
            case "runic_cost" -> "每点护盾消耗的风与地魔力百分之一单位，生效最小值0";
            case "notification_delay" -> "通知滚动间隔毫秒数";
            case "notification_max" -> "同时显示的通知数量上限";
            case "golem_link_quality" -> "傀儡连接标记质量；小于4关闭连接效果";
            default -> throw new IllegalArgumentException("Missing configuration translation: "+name);
        };
        return builder.comment(zh, description, "初始值 / Initial: "+defaultValue+"; 整数范围见下方 / Integer range below.").defineInRange(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
