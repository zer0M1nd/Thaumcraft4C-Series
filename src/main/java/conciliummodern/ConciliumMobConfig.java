package conciliummodern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;

public final class ConciliumMobConfig {
    public static final ForgeConfigSpec SPEC;
    public static final thaumcraft.api.config.RevivalConfig REVIVAL;
    public static final ForgeConfigSpec.IntValue MAD_WEIGHT,MAD_REPLACEMENT;
    public static final ForgeConfigSpec.IntValue PALADIN_REPLACEMENT;
    public static final ForgeConfigSpec.IntValue RANGER_WEIGHT;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> RANGER_BIOME_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> MAD_BIOME_BLACKLIST;
    public static final ForgeConfigSpec.IntValue THAUMATURGE_WEIGHT;
    public static final ForgeConfigSpec.IntValue QUICKSILVER_WEIGHT;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> QUICKSILVER_BIOME_BLACKLIST;
    public static final ForgeConfigSpec.IntValue RIFT_CHANCE;
    public static final ForgeConfigSpec.IntValue NETHER_EXPLORER_RATE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> THAUMATURGE_BIOME_BLACKLIST;
    static {
        var builder=new ForgeConfigSpec.Builder();
        REVIVAL=new thaumcraft.api.config.RevivalConfig(builder,true,()->tc4revival.config.TCConfig.SERVER.revival.profile());
        builder.push("mobs");
        RANGER_WEIGHT=builder.comment("红衣游侠自然生成权重，0禁用；需要神秘染色。", "Crimson ranger natural spawn weight; zero disables, requires Thaumic Dyes.").defineInRange("crimsonRangerSpawnChance",5,0,1000000);
        RANGER_BIOME_BLACKLIST=builder.comment("禁止红衣游侠自然生成的群系注册名。", "Biome registry names excluded from crimson ranger spawning.").defineListAllowEmpty("crimsonRangerBiomeBlacklist",java.util.List.of(),value->value instanceof String text&&ResourceLocation.tryParse(text)!=null);
        PALADIN_REPLACEMENT=builder.comment("新生成红衣骑士替换为圣骑士的概率，默认20%；仅腐化魔法联动启用时有效。", "Chance of replacing newly spawned cultist knights with paladins, default 20%; requires Tainted Magic.").defineInRange("crimsonPaladinReplacesCultistWarriorChance",20,0,100);
        QUICKSILVER_WEIGHT=builder.comment("水银元素生成权重，默认5，0禁用；重启生效。", "Quicksilver elemental spawn weight; default 5, zero disables, restart required.").defineInRange("quicksilverElementalSpawnChance",5,0,1000000);
        QUICKSILVER_BIOME_BLACKLIST=builder.comment("禁止水银元素自然生成的群系注册名。", "Biome registry names excluded from quicksilver elemental spawning.").defineListAllowEmpty("quicksilverElementalBiomeBlacklist",java.util.List.of(),value->value instanceof String text && ResourceLocation.tryParse(text)!=null);
        NETHER_EXPLORER_RATE=builder.comment("下界探索者生成尝试间隔，默认300 tick；0表示每tick。", "Nether explorer spawn attempt interval, default 300 ticks; zero means every tick.").defineInRange("netherExplorerSpawnRate",300,0,Integer.MAX_VALUE);
        THAUMATURGE_WEIGHT=builder.comment("魔导师自然生成权重；0 禁用，默认5，重启生效。", "Thaumaturge spawn weight; zero disables, default 5, restart required.")
                .defineInRange("thaumaturgeSpawnChance",5,0,1000000);
        THAUMATURGE_BIOME_BLACKLIST=builder.comment("禁止魔导师自然生成的群系注册名，默认空列表。", "Biome registry names excluded from thaumaturge spawning; default empty.")
                .defineListAllowEmpty("thaumaturgeBiomeBlacklist",java.util.List.of(),value->value instanceof String text && ResourceLocation.tryParse(text)!=null);
        MAD_WEIGHT=builder.comment("疯狂魔导师自然生成权重；0 禁用。默认5。重启生效。", "Mad thaumaturge spawn weight; zero disables. Default 5, restart required.")
                .defineInRange("madThaumaturgeSpawnChance",5,0,1000000);
        MAD_REPLACEMENT=builder.comment("新生成巨脑僵尸替换为疯狂魔导师的百分比；不会重新转换存档实体。默认20。", "Percentage of freshly spawned brainy zombies replaced; saved entities are never converted again. Default 20.")
                .defineInRange("madThaumaturgeReplacesBrainyZombieChance",20,0,100);
        MAD_BIOME_BLACKLIST=builder.comment("禁止疯狂魔导师自然生成的群系注册名。默认空列表。重启生效。", "Biome registry names excluded from natural mad thaumaturge spawning. Default empty, restart required.")
                .defineListAllowEmpty("madThaumaturgeBiomeBlacklist",java.util.List.of(),value->value instanceof String text && ResourceLocation.tryParse(text)!=null);
        builder.pop();builder.push("balance");
        RIFT_CHANCE=builder.comment("破坏节点生成裂隙的概率，默认100%。0会阻断原版生存进度。", "Rift chance when breaking a node, default 100 percent. Zero blocks original survival progression.")
                .defineInRange("chanceOfRiftOpening",100,0,100);
        builder.pop();SPEC=builder.build();REVIVAL.bind(SPEC);
    }
    private ConciliumMobConfig() {}
}
