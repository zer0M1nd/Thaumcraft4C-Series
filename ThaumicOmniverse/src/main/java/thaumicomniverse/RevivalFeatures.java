package thaumicomniverse;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.*;
import thaumcraft.api.research.*;

/** Gameplay groups only. Registration, save decoding, external resources and aspect supplementation are unconditional. */
public final class RevivalFeatures {
    public enum Group {
        MAID("touhou_little_maid", "车万女仆的法杖战斗、装备属性、符文护盾与充能管理；关闭后仍可取回已存放法杖。", "Touhou Little Maid wand combat, equipment, runic shielding and charging; deposited wands remain retrievable when disabled."),
        RESTORATION("thaumcraft6", "神秘时代六代设备与材料复刻。", "Devices and materials adapted from Thaumcraft 6."),
        THERMAL("thermal_dynamo", "热力系列源质能源炉。", "Thermal essentia dynamo."),
        NEURAL("neural_models", "深度怪物学习的神秘生物研究和外域预测产物配方。", "Hostile Neural Networks thaumic model research and Outer Lands prediction recipes."),
        SOUL("soul_automation", "灵魂核心绑定、安装与知识代理；关闭后保留绑定，仍可拆除核心。", "Soul core binding, installation and knowledge delegation; disabling retains bindings and permits removal."),
        AURA("aura_materials", "灵气颗粒、灵气块和液态灵气；材料独立于机械动力及附魔工艺。", "Aura granules, blocks and liquid aura; materials are independent of Create and Enchantment Industry."),
        UNIVERSAL("universal_alchemy", "通用晶簇化和金属嬗变；显式材料要素独立保留。", "Universal ore purification and metal transmutation; authored material aspects remain independent."),
        ENCHANTMENTS("enchantment_infusion", "第三方附魔的独立研究与注魔配方。", "Individual research and infusion recipes for third-party enchantments."),
        CHEMISTRY("alchemistry", "神秘材料的化学合成与分解配方。", "Alchemistry synthesis and decomposition of thaumic materials."),
        GREGTECH("gregtech", "格雷科技神秘材料、晶簇加工与源质发电设备。", "GregTech thaumic materials, cluster processing and essentia power."),
        RAILCRAFT("railcraft", "铁路神秘撬棍、源质矿车与装卸设备。", "Railcraft thaumic crowbars, essentia carts and loading equipment."),
        DRACONIC("draconic_modules", "龙之进化奥术模块，提供魔力减耗与潜能。", "Draconic Evolution arcane modules provide vis discounts and potency."),
        ALIGNMENT("thaumic_alignment", "神秘调谐；关闭时保留设备、材料与进度，暂停制作。", "Thaumic Alignment; disabling retains devices, materials and progress and pauses crafting."),
        TUBES("convenient_essentia", "炼金黄铜、源质伺服器与整套便捷源质管道；关闭后管道停止输送并保留存量。", "Alchemical brass, servos and the complete convenient essentia system. Disabled pipes stop transferring and retain contents."),
        COMPASSES("compasses", "邪术方尖碑与元始指南针定位。", "Sinister and primal compass location searches."),
        NODE_MAP("node_map", "已扫描节点地图；关闭后保留记录但停止显示和采集。", "Scanned-node map; disabling retains records but stops display and collection."),
        BACKPACKS("backpacks", "精妙背包的神秘升级、源质、法杖充能与法杖核心管理；关闭后保留库存。", "Sophisticated Backpack arcane upgrades, essentia, wand charging and focus management; inventories are retained."),
        BOTANIA("botania", "植物魔法揭示头盔、魔力墨水与护甲联动。", "Botania revealing helmets, mana ink and armor integration."),
        EVILCRAFT("evilcraft", "邪恶工艺血液杖端与血脉书写工具。", "EvilCraft blood wand cap and veined scribing tools."),
        MEKANISM("mekanism_tools", "通用机械的神秘金属多用工具。", "Mekanism thaumic-metal paxels."),
        FARMER("farmers_delight", "农夫乐事的神秘金属小刀。", "Farmer's Delight thaumic-metal knives."),
        TINKERS("tinkers", "匠魂神秘材料、特性和护甲效果。", "Tinkers' thaumic materials, traits and armor effects."),
        AVARITIA("avaritia", "无尽贪婪的元始珍珠、阿卡西记录、无限法杖与催化剂联动。", "Avaritia pearls, Akashic record, infinity wand and catalyst integration."),
        AVARITIA_CATALYST("avaritia_catalyst", "无尽催化剂增加极致元始珍珠门槛；依赖无尽贪婪联动。", "Add an extremely primordial pearl requirement to infinity catalysts; requires Avaritia integration.", RevivalConfig.Defaults.CHALLENGE),
        ATM_STORAGE("atm.storage", "ATM 金属压缩方块；中性存储便利在 Hardest 中保留，供证书进度使用。", "ATM compressed metal storage; neutral storage remains in Hardest to support certificate progression.", RevivalConfig.Defaults.CONVENIENCE),
        ATM_CERTIFICATE("atm.certificate", "ATM 证书及龙魂配方门槛；依赖 ATM 压缩存储。", "ATM certificate and dragon-soul recipe gate; requires ATM compressed storage.", RevivalConfig.Defaults.CHALLENGE),
        ATM_ORES("atm.ores", "ATM 维度中的神秘矿物生成；仅影响新生成区块。", "Thaumic ore generation in ATM dimensions; affects newly generated chunks only."),
        FORESTRY_BEES("forestry_bees", "林业魔法蜜蜂、蜂巢、突变、产物、效果与蜂箱设备。", "Forestry magic bees, hives, mutations, products, effects and apiary equipment."),
        PRODUCTIVE_BEES("productive_bees", "资源蜜蜂的神秘品种、转换与产物。", "Productive Bees thaumic species, conversions and products."),
        CROPS("resource_crops", "神秘农业资源作物及产物。", "Mystical Agriculture resource crops and products."),
        CREATE("create", "机械动力奥术机械、动态结构、法杖充能、源质与序列组装联动。", "Create arcane machinery, contraptions, wand charging, essentia and sequenced assembly integration."),
        INDUSTRIAL("industrial_foregoing", "工业先锋与神秘作物、源质、培育及生物处理联动。", "Industrial Foregoing thaumic crops, essentia, growing and mob processing."),
        EXTERNAL_HEAT("external_heat", "使用沉浸工程等外部热源加热神秘炼金设备。", "External heat, including Immersive Engineering, for thaumic alchemy equipment."),
        CHEMTHROWER("chemthrower", "沉浸工程化学喷射器使用神秘液体的效果。", "Immersive Engineering chemthrower effects for thaumic fluids."),
        MACHINERY("machinery_ports", "模块化机械的灵气与源质接口和配方要求；关闭后相关配方不能免费运行。", "Modular Machinery vis/essentia ports and requirements; disabled requirements never become free recipes."),
        FOOD("food_integrations", "潘马斯等食物系统与神秘食物的玩法联动；要素补充独立保留。", "Gameplay integration with food systems such as Pam's; aspect supplementation remains independent.");

        final String key, chinese, english;
        final RevivalConfig.Defaults defaults;
        private RevivalConfig.Option option;
        Group(String key, String chinese, String english) { this(key,chinese,english,RevivalConfig.Defaults.BENEFIT); }
        Group(String key, String chinese, String english, RevivalConfig.Defaults defaults) {
            this.key=key;this.chinese=chinese;this.english=english;this.defaults=defaults;
        }
        public boolean enabled() {
            // Force initialization before integrations (e.g. Forestry plugins) ask for defaults.
            if (!CONFIG.option(key).get()) return false;
            return (this != ATM_CERTIFICATE || ATM_STORAGE.enabled())
                    && (this != AVARITIA_CATALYST || AVARITIA.enabled());
        }
        public RevivalConfig.Option option() { return CONFIG.option(key); }
    }
    public static final RevivalConfig CONFIG;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue THERMAL_DRAGONSTEEL, THERMAL_ABYSSAL;
    public static final ForgeConfigSpec.BooleanValue ANCIENT_FOREST;
    public static final ForgeConfigSpec.DoubleValue ESSENTIA_FUEL_DURATION;
    public static final ForgeConfigSpec.IntValue AIRSHIP_FUEL_TICKS;
    public static final ForgeConfigSpec.DoubleValue AIRSHIP_TURN_SPEED,AIRSHIP_BEND_LENGTH,AIRSHIP_DEFAULT_BEND_RADIUS,AIRSHIP_SPEED_MULTIPLIER,AIRSHIP_POWERED_SPEED_MULTIPLIER;
    static {
        var builder=new ForgeConfigSpec.Builder();
        CONFIG=new RevivalConfig(builder,true,()->tc4revival.config.TCConfig.SERVER.revival.profile());
        for(var group:Group.values()) group.option=CONFIG.feature(group.key,group.chinese,group.english,group.defaults);
        builder.push("thermal_extra");
        THERMAL_DRAGONSTEEL=builder.comment("龙钢配方以神秘锭替代镍锭，默认关闭。 / Replace nickel with thaumium in dragonsteel; disabled by default.").define("replace_dragonsteel_ingredient",false);
        THERMAL_ABYSSAL=builder.comment("深渊配方以虚空种子替代钻石粉，默认关闭。 / Replace diamond dust with void seeds in abyssal ingots; disabled by default.").define("replace_abyssal_ingredient",false);
        builder.pop();
        builder.push("gregtech");
        ESSENTIA_FUEL_DURATION=builder.comment("源质燃料燃烧时间倍率，默认是原时长的 4 倍；停服修改后重启世界/服务器。 / Essentia fuel duration multiplier; edit while stopped, then restart the world/server.").defineInRange("essentia_fuel_duration_multiplier",4d,.01d,1000d);
        builder.pop();
        builder.push("worldgen");
        ANCIENT_FOREST=builder.comment("远古魔法森林：默认关闭；启用后仅在魔法森林内部生成，并强制关闭旧版蓝色魔法森林。需重启世界。 / Ancient Magical Forest interiors; disabled by default. Restart the world to apply.")
                .worldRestart().define("ancient_magical_forest",false);
        builder.pop();
        builder.push("airship");
        AIRSHIP_SPEED_MULTIPLIER=builder.comment("飞艇全局速度乘数：未加速，相对列车，同时影响最大加速度。 / Unpowered airship speed and acceleration relative to trains.").defineInRange("global_speed_multiplier",.25d,0d,100d);
        AIRSHIP_POWERED_SPEED_MULTIPLIER=builder.comment("飞艇全局速度乘数：加速，相对加速列车，同时影响最大加速度。 / Powered airship speed and acceleration relative to powered trains.").defineInRange("global_powered_speed_multiplier",.25d,0d,100d);
        AIRSHIP_TURN_SPEED=builder.comment("最大三维转向速度（度/秒），加速供魔不会提高此限制。 / Maximum 3D steering degrees per second, unaffected by boost.").defineInRange("maximum_turn_degrees_per_second",10d,.1d,360d);
        AIRSHIP_BEND_LENGTH=builder.comment("编辑临时弯道时允许的最大长度（格）。 / Maximum edited junction bend length in blocks.").defineInRange("maximum_edited_bend_length",40d,1d,1024d);
        AIRSHIP_DEFAULT_BEND_RADIUS=builder.comment("临时弯道默认曲率半径（格），仅用于默认生成；实际半径可因几何和长度限制小于或大于此值，不限制手动编辑。 / Preferred radius for generated bends only, not a minimum editing constraint.").defineInRange("default_bend_radius",2d,.05d,128d);
        AIRSHIP_FUEL_TICKS=builder.comment("每点能量源质支持的加速飞行 tick 数，仅在移动时消耗计时；默认 1200（60 秒）。 / Moving boost ticks per point of potentia.").defineInRange("potentia_boost_ticks",1200,20,72000);
        builder.pop();
        SPEC=builder.build();CONFIG.bind(SPEC);
    }
    public static void register() {
        FeatureRecipeCondition.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,SPEC,"thaumicomniverse-revival-server.toml");
        for(var group:Group.values()) FeatureRecipes.register(new ResourceLocation("thaumicomniverse",group.key.replace('.','/')),
                id->recipeGroup(id)==group&&!id.toString().equals("thaumicomniverse:soul_core_binding"),group::enabled);
        FeatureRecipes.register(ResourceLocation.parse("thaumicomniverse:pearl_fragments"),
                id->id.toString().equals("thaumicomniverse:collab/primordial_pearl_from_fragments"),
                ()->Group.CROPS.enabled()||Group.PRODUCTIVE_BEES.enabled());
        ResearchCategories.registerEnablementFilter(ResourceLocation.parse("thaumicomniverse:features"),RevivalFeatures::researchEnabled);
        ResearchPrerequisites.register(ResourceLocation.parse("thaumicomniverse:features"),(entry,catalog,parents)->new ResearchPrerequisites.Parents(
                parents.visible().stream().filter(RevivalFeatures::researchEnabled).toList(),
                parents.hidden().stream().filter(RevivalFeatures::researchEnabled).toList()));
    }
    public static boolean researchEnabled(String key) {
        if(key.equals("thaumicomniverse:ESSENTIA_SWITCH"))return false;
        if(FeatureOwnership.migratedResearch(key))return true;
        var group=researchGroup(key);return group==null||group.enabled();
    }
    public static Group researchGroup(String key) {
        if(key.equals("ALCHEMICALBRASS")||key.equals("CONVENIENTTUBES"))return Group.TUBES;
        if(!key.startsWith("thaumicomniverse:"))return null;
        var name=key.substring(key.indexOf(':')+1);
        if(name.startsWith("AIRSHIP"))return Group.CREATE;
        if(name.equals("GUIDE_CREATE"))return Group.CREATE;
        if(name.equals("SOLID_AURA"))return Group.AURA;
        if(name.startsWith("RESTORED_"))return Group.RESTORATION;
        if(name.equals("ESSENTIA_DYNAMO"))return Group.THERMAL;
        if(name.equals("NEURAL_MODELS"))return Group.NEURAL;
        if(name.equals("KINETIC_FOCUS")||name.equals("RELOCATION_FOCUS")||name.equals("THAUMIC_CRUSHING_WHEEL"))return Group.CREATE;
        if(name.startsWith("ENCHANT/")||name.equals("UNIVERSAL_ENCHANTMENTS"))return Group.ENCHANTMENTS;
        if(name.startsWith("MB_")||name.equals("MAGIC_BEES"))return Group.FORESTRY_BEES;
        if(name.endsWith("_UPGRADE"))return Group.BACKPACKS;
        return switch(name) {
            case "alignment" -> Group.ALIGNMENT;
            case "SOUL_CORE" -> Group.SOUL;
            case "UNIVERSAL_CLUSTERS","UNIVERSAL_TRANSMUTATION" -> Group.UNIVERSAL;
            case "ARCANE_MODULES" -> Group.DRACONIC;
            case "ARCANE_ASSEMBLY","SOLID_AURA","ARCANE_CONTRAPTIONS","THAUMIC_CASING","ARCANE_MOTOR","ARCANE_BURNER","THAUMIC_CRANK","VOID_MECHANISMS","WAND_CHARGER","ARCANE_SUSPENSION","ARCANE_MAGNET","FLOATING_DEPOT","ESSENTIA_SPOUT","ESSENTIA_SWITCH","PORTABLE_ESSENTIA" -> Group.CREATE;
            case "GT_MATERIALS","ESSENTIA_ENGINE" -> Group.GREGTECH;
            case "ESSENTIA_RAILWAY" -> Group.RAILCRAFT;
            case "SINISTER_COMPASS","PRIMAL_COMPASS" -> Group.COMPASSES;
            case "SCANNED_NODE_MAP" -> Group.NODE_MAP;
            case "MACHINE_ASPECT_PORTS" -> Group.MACHINERY;
            case "THAUMATURGE_CERTIFICATE" -> Group.ATM_CERTIFICATE;
            case "BIG_PEARL","AKASHIC" -> Group.AVARITIA;
            default -> null;
        };
    }
    public static Group recipeGroup(ResourceLocation id) {
        if(FeatureOwnership.migratedRecipe(id.toString()))return null;
        String path=id.getPath();
        if(id.getNamespace().equals("thaumicomniverse")&&java.util.Set.of("expansion/solid_aura_block","expansion/solid_aura").contains(path))return Group.AURA;
        if(id.getNamespace().equals("thaumicomniverse")&&(path.equals("atm/alchemical_brass_block")||path.equals("atm/alchemical_brass_block_unpack")))return Group.RESTORATION;
        if(id.getNamespace().equals("thaumicomniverse")&&path.startsWith("restored/"))return Group.RESTORATION;
        if(id.getNamespace().equals("thaumicomniverse")&&path.startsWith("thermal/"))return Group.THERMAL;
        if(id.getNamespace().equals("thaumicomniverse")&&path.startsWith("neural/"))return Group.NEURAL;
        if(id.getNamespace().equals("mysticalagriculture")&&path.contains("omniverse_"))return Group.CROPS;
        if(id.getNamespace().equals("productivebees")&&path.startsWith("centrifuge/thaumicomniverse/"))return Group.PRODUCTIVE_BEES;
        if(!id.getNamespace().equals("thaumicomniverse"))return null;
        if(path.startsWith("tinkers/"))return Group.TINKERS;
        if(path.startsWith("meat_treat/"))return Group.FOOD;
        if(path.startsWith("collab/infinity_wand"))return Group.AVARITIA;
        if(path.startsWith("universal/"))return Group.UNIVERSAL;
        if(path.startsWith("enchantment/"))return Group.ENCHANTMENTS;
        if(path.startsWith("expansion/chemistry/"))return Group.CHEMISTRY;
        if(path.startsWith("expansion/laser/"))return Group.INDUSTRIAL;
        if(path.equals("expansion/liquid_aura"))return Group.CREATE;
        if(path.equals("expansion/nucleosynthesis_pearl"))return Group.MEKANISM;
        if(path.equals("soul_core_binding"))return Group.SOUL;
        if(path.startsWith("arcane/convenient_")||path.equals("arcane/essentia_servo")||path.equals("crucible/alchemical_brass"))return Group.TUBES;
        if(path.startsWith("atm/"))return path.endsWith("thaumaturge_certificate")?Group.ATM_CERTIFICATE:Group.ATM_STORAGE;
        if(path.startsWith("collab/crops/"))return Group.CROPS;
        if(path.startsWith("collab/machinery/"))return Group.MACHINERY;
        if(path.startsWith("gt/"))return Group.GREGTECH;
        if(path.startsWith("create/")||path.startsWith("generated_create/")||path.startsWith("airship/"))return Group.CREATE;
        if(path.startsWith("railcraft/"))return Group.RAILCRAFT;
        if(path.startsWith("collab/bees/"))return path.endsWith("_conversion")||path.endsWith("_produce")?Group.PRODUCTIVE_BEES:Group.FORESTRY_BEES;
        return itemGroup(path.substring(path.lastIndexOf('/')+1));
    }
    public static boolean itemEnabled(String name) {
        if(name.equals("airship_core"))return false;
        if(name.equals("essentia_stockpile_switch"))return false; // legacy decoding/migration only
        if(name.equals("alchemical_brass_sheet"))return Group.CREATE.enabled()&&Group.TUBES.enabled();
        if(name.equals("primordial_fragment"))return Group.CROPS.enabled()||Group.PRODUCTIVE_BEES.enabled();
        var group=itemGroup(name);return group==null||group.enabled();
    }
    public static Group itemGroup(String name) {
        if(name.startsWith("airship_"))return Group.CREATE;
        if(java.util.Set.of("solid_aura","solid_aura_block","aura_bucket","aura","flowing_aura").contains(name))return Group.AURA;
        if(name.equals("focus_kinetic")||name.equals("focus_relocation")||name.equals("thaumic_crushing_wheel")||name.equals("incomplete_thaumic_crushing_wheel"))return Group.CREATE;
        if(name.equals("arcane_crafter"))return Group.RESTORATION;
        if(FeatureOwnership.migratedItem(name))return null;
        if(name.equals("outerlands_prediction"))return Group.NEURAL;
        if(name.equals("essentia_dynamo"))return Group.THERMAL;
        if(name.equals("alchemical_brass_sheet"))return Group.CREATE;
        if(java.util.Set.of("thaumic_casing","thaumic_magnetic_casing","arcane_motor","thaumic_hand_crank","empty_arcane_blaze_burner","arcane_blaze_burner","void_sandpaper","portable_essentia_interface","wand_charger","magic_hand","void_backtank","void_backtank_placeable","essentia_stockpile_switch","arcane_suspension_coupler","arcane_kinetic_magnet","arcane_floating_depot","essentia_spout","incomplete_essentia_infusion").contains(name))return Group.CREATE;
        if(name.equals("soul_core"))return Group.SOUL;
        if(name.equals("ore_cluster"))return Group.UNIVERSAL;
        if(name.contains("essentia_input_hatch")||name.equals("arcane_machine_casing")||name.equals("large_essentia_generator"))return Group.GREGTECH;
        if(name.endsWith("_crowbar")||name.equals("essentia_minecart")||name.equals("essentia_loader")||name.equals("essentia_unloader"))return Group.RAILCRAFT;
        if(name.endsWith("alchemical_mechanism")||name.endsWith("infused_mechanism")||java.util.Set.of("solid_aura","solid_aura_block","aura_bucket","aura").contains(name))return Group.CREATE;
        if(name.endsWith("_arcane") && name.startsWith("item_"))return Group.DRACONIC;
        if(java.util.Set.of("alignment_core", "positive_pedestal", "negative_pedestal", "stack_positive_pedestal", "stack_negative_pedestal", "aligned_crystal").contains(name))return Group.ALIGNMENT;
        if(name.startsWith("convenient_")||name.equals("essentia_servo")||name.equals("alchemical_brass")||name.equals("alchemical_brass_ingot"))return Group.TUBES;
        if(name.contains("compass"))return Group.COMPASSES;
        if(name.endsWith("_upgrade"))return Group.BACKPACKS;
        if(name.startsWith("machine_")||name.equals("vis_bridge"))return Group.MACHINERY;
        if(name.contains("revealing_helmet")||name.equals("mana_inkwell"))return Group.BOTANIA;
        if(name.startsWith("veined_scribing_tools")||name.equals("blood_wand_cap"))return Group.EVILCRAFT;
        if(name.endsWith("_paxel"))return Group.MEKANISM;
        if(name.endsWith("_knife"))return Group.FARMER;
        if(name.equals("thaumium_material") || name.startsWith("tinkers/"))return Group.TINKERS;
        if(name.equals("warp_ward"))return Group.BOTANIA;
        if(name.equals("akashic_record")||name.equals("infinity_wand")||name.equals("extremely_primordial_pearl"))return Group.AVARITIA;
        if(name.equals("thaumaturge_certificate"))return Group.ATM_CERTIFICATE;
        if(name.contains("_block"))return Group.ATM_STORAGE;
        if(name.startsWith("bee_")||name.startsWith("frame_")||name.equals("magic_apiary")||name.equals("vis_aura_provider")||name.equals("thaumium_scoop")||name.equals("thaumium_grafter"))return Group.FORESTRY_BEES;
        return null;
    }
    private RevivalFeatures() {}
}
