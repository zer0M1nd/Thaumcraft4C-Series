package tc4revival.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tc4revival.aspect.item.ItemAspectRule;

/** Direct vanilla registrations from ConfigAspects, with explicit modern item splits. */
public  class LegacyItemDefinitions {
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak");
    private static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private LegacyItemDefinitions() {}

    public static List<String> legacyDyes() {
        return minecraft("ink_sac red_dye green_dye cocoa_beans lapis_lazuli purple_dye cyan_dye light_gray_dye gray_dye pink_dye lime_dye yellow_dye light_blue_dye magenta_dye orange_dye bone_meal");
    }

    public static List<ItemAspectRule> rules() {
        var rows = new ArrayList<ItemAspectRule>();
        tagged(rows, "forge:stone", "stone", "terra=2");
        tagged(rows, "forge:cobblestone", "cobblestone", "terra=1 perditio=1");
        var logs = new ArrayList<String>();
        logs.addAll(variants(WOODS, "_log"));
        logs.addAll(variants(WOODS, "_wood"));
        rows.add(rule(logs, "minecraft:logs", "arbor=4"));
        rows.add(rule(variants(WOODS, "_planks"), "minecraft:planks", "arbor=1"));
        rows.add(rule(variants(WOODS, "_slab"), "minecraft:wooden_slabs", "arbor=1"));
        rows.add(rule(variants(WOODS, "_stairs"), "minecraft:wooden_stairs", "arbor=1"));
        tagged(rows, "forge:rods/wooden", "stick", "arbor=1");
        rows.add(rule(variants(WOODS, "_sapling"), "minecraft:saplings", "arbor=1 herba=2"));
        rows.add(rule(variants(WOODS, "_leaves"), "minecraft:leaves", "herba=1"));
        rows.add(rule(legacyDyes(), "forge:dyes", "sensus=1"));
        tagged(rows, "forge:ores/lapis", "lapis_ore", "terra=1 sensus=3");
        tagged(rows, "forge:ores/diamond", "diamond_ore", "terra=1 lucrum=3 vitreus=3");
        tagged(rows, "forge:gems/diamond", "diamond", "vitreus=4 lucrum=4");
        tagged(rows, "forge:ores/redstone", "redstone_ore", "terra=1 potentia=2 machina=2");
        tagged(rows, "forge:ores/emerald", "emerald_ore", "terra=1 lucrum=4 vitreus=3");
        tagged(rows, "forge:gems/emerald", "emerald", "vitreus=4 lucrum=5");
        tagged(rows, "forge:ores/quartz", "nether_quartz_ore", "terra=1 vitreus=3");
        direct(rows, "quartz", "vitreus=1 potentia=1");
        direct(rows, "gold_nugget", "metallum=1");
        tagged(rows, "forge:nuggets/iron", "iron_nugget", "metallum=1");
        tagged(rows, "forge:ores/iron", "iron_ore", "terra=1 metallum=3");
        direct(rows, "iron_ingot", "metallum=4");
        tagged(rows, "forge:ores/gold", "gold_ore", "terra=1 metallum=2 lucrum=1");
        direct(rows, "gold_ingot", "metallum=3 lucrum=2");
        direct(rows, "coal_ore", "terra=1 potentia=2 ignis=1");
        direct(rows, "coal charcoal", "potentia=2 ignis=2");
        tagged(rows, "forge:dusts/redstone", "redstone", "potentia=2 machina=1");
        tagged(rows, "forge:dusts/glowstone", "glowstone_dust", "sensus=1 lux=2");
        direct(rows, "glowstone", "sensus=3 lux=10");
        // Copper is the explicit necessary-material exception, including both current ore tag members.
        tagged(rows, "forge:ingots/copper", "copper_ingot", "metallum=3 permutatio=1");
        tagged(rows, "forge:ores/copper", "copper_ore deepslate_copper_ore", "metallum=2 terra=1 permutatio=1");
        // The plain TC-only observer has copper disabled; restore these source-defined conditional values.
        rows.add(rule(List.of("thaumcraft:copper_nugget"), "forge:nuggets/copper", "metallum=1"));
        rows.add(rule(List.of("thaumcraft:copper_cluster"), null, "ordo=1 metallum=5 terra=1 permutatio=2"));
        direct(rows, "bedrock", "vacuos=16 perditio=16 terra=16 tenebrae=16");
        direct(rows, "dirt coarse_dirt", "terra=2");
        direct(rows, "podzol", "terra=1 herba=1");
        direct(rows, "farmland", "terra=1 meto=2");
        direct(rows, "sand red_sand", "terra=1 perditio=1");
        direct(rows, "grass_block mycelium", "terra=1 herba=1");
        direct(rows, "clay_ball", "aqua=1 terra=1");
        direct(rows, "end_stone", "terra=1 tenebrae=1");
        direct(rows, "gravel", "terra=2");
        direct(rows, "clay", "terra=3 aqua=3");
        direct(rows, "terracotta", "terra=4 ignis=1");
        rows.add(rule(variants(COLORS, "_terracotta"), null, "terra=3 ignis=1 sensus=1"));
        direct(rows, "brick", "terra=1 ignis=1");
        direct(rows, "nether_brick", "ignis=1");
        direct(rows, "soul_sand", "terra=1 vinculum=1 spiritus=1");
        direct(rows, "netherrack", "terra=2 ignis=1");
        direct(rows, "nether_bricks", "terra=2 ignis=1");
        direct(rows, "glass", "vitreus=1");
        rows.add(rule(variants(COLORS, "_stained_glass"), null, "vitreus=1"));
        direct(rows, "mossy_cobblestone", "terra=1 herba=1 praecantatio=1");
        direct(rows, "obsidian", "terra=2 ignis=2 tenebrae=1");
        direct(rows, "infested_stone infested_cobblestone infested_stone_bricks infested_mossy_stone_bricks infested_cracked_stone_bricks infested_chiseled_stone_bricks", "terra=2 bestia=1 vinculum=1");
        direct(rows, "stone_bricks", "terra=2");
        direct(rows, "mossy_stone_bricks", "terra=1 herba=1");
        direct(rows, "cracked_stone_bricks", "terra=1 perditio=1");
        direct(rows, "chiseled_stone_bricks", "terra=1 ordo=1");
        direct(rows, "grass fern sunflower lilac tall_grass large_fern rose_bush peony", "herba=1 aer=1");
        direct(rows, "lily_pad", "herba=2 aqua=1");
        direct(rows, "dead_bush", "herba=1 perditio=1");
        direct(rows, "vine wheat_seeds melon_seeds pumpkin_seeds", "herba=1");
        direct(rows, "melon_slice", "fames=1");
        direct(rows, "nether_wart", "herba=1 praecantatio=1");
        direct(rows, "poppy blue_orchid allium azure_bluet red_tulip orange_tulip white_tulip pink_tulip oxeye_daisy dandelion", "herba=1 victus=1 sensus=1");
        direct(rows, "cactus", "herba=3 aqua=1 perditio=1");
        direct(rows, "brown_mushroom", "herba=1 tenebrae=1 terra=1");
        direct(rows, "red_mushroom", "herba=1 tenebrae=1 ignis=1");
        direct(rows, "brown_mushroom_block", "herba=2 tenebrae=1 terra=1");
        direct(rows, "red_mushroom_block", "herba=2 tenebrae=1 ignis=1");
        direct(rows, "sugar_cane", "herba=1 aqua=1 aer=1");
        direct(rows, "wheat apple", "messis=2 fames=1");
        direct(rows, "carrot", "messis=1 fames=1 sensus=1");
        direct(rows, "potato", "messis=1 fames=1 terra=1");
        direct(rows, "baked_potato", "messis=1 fames=2");
        direct(rows, "poisonous_potato", "messis=1 venenum=2");
        direct(rows, "carved_pumpkin", "messis=2");
        direct(rows, "name_tag", "cognitio=2 bestia=2");
        direct(rows, "iron_horse_armor", "tutamen=2 bestia=2");
        direct(rows, "golden_horse_armor", "tutamen=4 bestia=2");
        direct(rows, "diamond_horse_armor", "tutamen=6 bestia=2");
        direct(rows, "ice", "gelum=4");
        direct(rows, "packed_ice", "gelum=3 terra=1");
        direct(rows, "snowball", "gelum=1");
        direct(rows, "cookie", "fames=1");
        direct(rows, "torch", "lux=1");
        direct(rows, "cobweb", "vinculum=2 pannus=1");
        direct(rows, "flint", "terra=1 instrumentum=1");
        direct(rows, "string", "bestia=1 pannus=1");
        direct(rows, "slime_ball", "limus=2");
        direct(rows, "leather", "pannus=2 bestia=1 tutamen=1");
        direct(rows, "rotten_flesh", "humanus=1 corpus=2");
        direct(rows, "feather", "volatus=2 aer=1");
        direct(rows, "bone", "mortuus=2 corpus=1");
        direct(rows, "egg", "limus=1 victus=1 bestia=1");
        direct(rows, "spider_eye", "sensus=2 bestia=2 venenum=2");
        direct(rows, "gunpowder", "ignis=4 perditio=4");
        rows.add(rule(variants(COLORS, "_wool"), null, "pannus=4 fabrico=1"));
        direct(rows, "cod salmon tropical_fish pufferfish", "corpus=3 victus=1 aqua=1");
        direct(rows, "cooked_cod cooked_salmon cooked_chicken", "fabrico=1 corpus=4 fames=3");
        direct(rows, "chicken", "corpus=3 victus=2 bestia=1");
        direct(rows, "porkchop", "corpus=3 victus=1 bestia=1");
        direct(rows, "cooked_porkchop", "fabrico=1 corpus=3 fames=3");
        direct(rows, "beef", "corpus=4 victus=2 bestia=1");
        direct(rows, "cooked_beef", "fabrico=1 corpus=4 fames=4");
        direct(rows, "blaze_rod", "ignis=4 praecantatio=2");
        direct(rows, "saddle", "bestia=2 pannus=3 iter=3");
        direct(rows, "ender_pearl", "alienis=4 praecantatio=2 iter=4");
        direct(rows, "ghast_tear", "aqua=1 exanimis=4 spiritus=4");
        direct(rows, "skeleton_skull", "mortuus=4 spiritus=4 exanimis=4");
        direct(rows, "wither_skeleton_skull", "mortuus=4 spiritus=4 exanimis=4 venenum=4");
        direct(rows, "zombie_head", "mortuus=4 spiritus=4 corpus=4");
        direct(rows, "player_head", "mortuus=4 spiritus=4 humanus=4");
        direct(rows, "creeper_head", "mortuus=4 spiritus=4 perditio=2 ignis=2");
        direct(rows, "music_disc_11", "sensus=4 aer=4 lucrum=4");
        disc(rows, "13", "aqua");
        disc(rows, "cat", "bestia");
        disc(rows, "chirp", "terra");
        disc(rows, "far", "alienis");
        disc(rows, "mall", "humanus");
        disc(rows, "mellohi", "fabrico");
        disc(rows, "stal", "tenebrae");
        disc(rows, "strad", "machina");
        disc(rows, "ward", "praecantatio");
        disc(rows, "blocks", "instrumentum");
        disc(rows, "wait", "vinculum");
        direct(rows, "nether_star", "alienis=8 praecantatio=8 ordo=8 lux=8");
        direct(rows, "chainmail_helmet", "metallum=8");
        direct(rows, "chainmail_chestplate", "metallum=12");
        direct(rows, "chainmail_leggings", "metallum=11");
        direct(rows, "chainmail_boots", "metallum=7");
        direct(rows, "dragon_egg", "alienis=8 bestia=8 praecantatio=8");
        direct(rows, "end_portal_frame", "alienis=4 machina=4 iter=4");
        direct(rows, "spawner", "bestia=4 iter=4 exanimis=4 praecantatio=4");
        direct(rows, "bucket", "metallum=8 vacuos=1");
        direct(rows, "water_bucket", "metallum=8 vacuos=1 aqua=4");
        direct(rows, "lava_bucket", "metallum=8 vacuos=1 ignis=4 terra=1");
        direct(rows, "milk_bucket", "metallum=8 vacuos=1 fames=2 sano=2 aqua=2");
        direct(rows, "cake", "aqua=4 fames=4 victus=4");
        direct(rows, "crafting_table", "fabrico=4");
        direct(rows, "anvil chipped_anvil damaged_anvil", "metallum=64 fabrico=2 instrumentum=2");
        direct(rows, "tripwire_hook", "sensus=1 machina=1 vinculum=1");
        return List.copyOf(rows);
    }

    private static void disc(List<ItemAspectRule> rows, String name, String aspect) {
        direct(rows, "music_disc_" + name, "sensus=4 aer=4 " + aspect + "=4 lucrum=4");
    }

    private static void direct(List<ItemAspectRule> rows, String names, String amounts) { tagged(rows, null, names, amounts); }
    private static void tagged(List<ItemAspectRule> rows, String tag, String names, String amounts) {
        rows.add(rule(minecraft(names), tag, amounts));
    }

    private static List<String> minecraft(String names) {
        return Arrays.stream(names.split(" ")).map(name -> "minecraft:" + name).toList();
    }

    private static List<String> variants(List<String> prefixes, String suffix) {
        return prefixes.stream().map(prefix -> "minecraft:" + prefix + suffix).toList();
    }

    private static ItemAspectRule rule(List<String> items, String tag, String amounts) {
        var quantities = Arrays.stream(amounts.split(" ")).map(pair -> {
            var parts = pair.split("=");
            return new ItemAspectRule.Quantity(parts[0], Integer.parseInt(parts[1]));
        }).toList();
        return new ItemAspectRule(items, tag, quantities);
    }
}
