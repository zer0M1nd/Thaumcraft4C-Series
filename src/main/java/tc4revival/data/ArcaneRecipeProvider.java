package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.content.WandParts;

public  class ArcaneRecipeProvider implements DataProvider {
    private final PackOutput output;
    public ArcaneRecipeProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes = new ArrayList<CompletableFuture<?>>();
        var label=new JsonObject();label.addProperty("type","thaumcraft:jar_label");label.addProperty("source_amount",8);
        var labelInput=new JsonObject();labelInput.addProperty("item","thaumcraft:jar_label");label.add("label",labelInput);label.add("result",labelInput.deepCopy());
        var phialInput=new JsonObject();phialInput.addProperty("item","thaumcraft:essentia_phial");label.add("source",phialInput);
        writes.add(DataProvider.saveStable(cache,label,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/recipes/jar_label_aspect.json")));
        writes.add(save(cache,"alchemy_centrifuge",shaped("CENTRIFUGE","alchemy_centrifuge",1,new String[]{" T ","ACP"," T "},Map.of("T","thaumcraft:essentia_tube","A","thaumcraft:alembic","C","thaumcraft:alchemical_construct","P","minecraft:piston"),Map.of("aqua",5,"ordo",5,"perditio",5))));
        writes.add(save(cache,"essentia_crystallizer",shaped("ESSENTIACRYSTAL","essentia_crystallizer",1,new String[]{"IDI","QCQ","WTW"},Map.of("I","#forge:ingots/iron","D","minecraft:dispenser","Q","thaumcraft:balanced_shard","C","thaumcraft:alchemical_construct","W","#minecraft:planks","T","thaumcraft:essentia_tube"),Map.of("aqua",5,"terra",15,"ordo",5))));
        writes.add(save(cache,"bone_bow",shaped("BONEBOW","bone_bow",1,new String[]{"SB ","SEB","SB "},Map.of("S","minecraft:string","B","minecraft:bone","E","thaumcraft:entropy_shard"),Map.of("aer",16,"perditio",32))));
        String[] arrowAspects={"aer","ignis","aqua","terra","ordo","perditio"};
        for (int element=0;element<6;element++) {
            String name=tc4revival.content.ShardType.values()[element].material();
            writes.add(save(cache,"primal_arrow_"+name,shaped("PRIMALARROW","primal_arrow_"+name,8,new String[]{"AAA","ASA","AAA"},Map.of("A","minecraft:arrow","S","thaumcraft:"+name+"_shard"),Map.of(arrowAspects[element],8))));
        }
        writes.add(save(cache,"hungry_chest",shaped("HUNGRYCHEST","hungry_chest",1,new String[]{"WTW","W W","WWW"},Map.of("W","#minecraft:planks","T","minecraft:oak_trapdoor"),Map.of("aer",5,"ordo",3,"perditio",3))));
        writes.add(save(cache,"golem_fetter",shaped("GOLEMFETTER","golem_fetter",1,new String[]{"SSS","IRI","BBB"},Map.of("S","thaumcraft:arcane_stone","I","minecraft:iron_ingot","R","minecraft:redstone_block","B","thaumcraft:arcane_stone_bricks"),Map.of("terra",5,"ordo",5))));
        writes.add(save(cache,"golem_bell",shaped("GOLEMBELL","golem_bell",1,new String[]{" QQ"," QQ","S  "},Map.of("S","#forge:rods/wooden","Q","minecraft:quartz"),Map.of("ordo",5))));
        writes.add(save(cache,"golem_core_blank",shaped("COREGATHER","golem_core_blank",1,new String[]{" C ","CNC"," C "},Map.of("C","minecraft:brick","N","thaumcraft:nitor"),Map.of("ordo",5,"ignis",5))));
        writes.add(save(cache,"deconstruction_table",undamaged(shaped("DECONSTRUCTOR","deconstruction_table",1,new String[]{" S ","ATP"},Map.of("S","thaumcraft:thaumometer","T","thaumcraft:table","A","minecraft:golden_axe","P","minecraft:golden_pickaxe"),Map.of("perditio",20)),"minecraft:golden_axe","minecraft:golden_pickaxe")));
        writes.add(save(cache,"flux_scrubber",shaped("FLUXSCRUB","flux_scrubber",1,new String[]{" B ","GOG","STS"},Map.of("B","thaumcraft:bellows","G","minecraft:iron_bars","O","thaumcraft:vis_filter","T","thaumcraft:essentia_tube","S","thaumcraft:arcane_stone_bricks"),Map.of("aqua",16,"ordo",16,"aer",8))));
        writes.add(save(cache,"arcane_spa",shaped("ARCANESPA","arcane_spa",1,new String[]{"QIQ","SJS","SPS"},Map.of("Q","minecraft:quartz_block","I","minecraft:iron_bars","S","thaumcraft:arcane_stone","J","thaumcraft:essentia_jar","P","minecraft:piston"),Map.of("aqua",16,"ordo",8,"terra",4))));
        writes.add(save(cache,"travel_paving_stone",shaped("PAVETRAVEL","travel_paving_stone",4,new String[]{"SAS","SBS"},Map.of("S","thaumcraft:arcane_stone_bricks","A","thaumcraft:air_shard","B","thaumcraft:earth_shard"),Map.of("terra",10,"aer",10))));
        writes.add(save(cache,"warding_paving_stone",shaped("PAVEWARD","warding_paving_stone",4,new String[]{"SAS","SBS"},Map.of("S","thaumcraft:arcane_stone_bricks","A","thaumcraft:fire_shard","B","thaumcraft:order_shard"),Map.of("ignis",10,"ordo",10))));
        String[] elements={"air","earth","fire","water","order","entropy"},aspects={"aer","terra","ignis","aqua","ordo","perditio"};
        for (int i=0;i<elements.length;i++) writes.add(save(cache,"golem_upgrade_"+elements[i],shaped("UPGRADE"+elements[i].toUpperCase(java.util.Locale.ROOT),"golem_upgrade_"+elements[i],1,new String[]{"NNN","NCN","NNN"},Map.of("N","minecraft:gold_nugget","C","thaumcraft:"+elements[i]+"_shard"),Map.of(aspects[i],10))));
        writes.add(save(cache,"golem_decoration_hat",shaped("TINYHAT","golem_decoration_hat",1,new String[]{" C "," G ","CCC"},Map.of("C","minecraft:black_wool","G","minecraft:gold_ingot"),Map.of("ordo",8,"ignis",8))));
        writes.add(save(cache,"golem_decoration_fez",shaped("TINYFEZ","golem_decoration_fez",1,new String[]{"CCS","CCS","  S"},Map.of("C","minecraft:red_wool","S","minecraft:string"),Map.of("aqua",4,"terra",4))));
        writes.add(save(cache,"golem_decoration_bowtie",shaped("TINYBOWTIE","golem_decoration_bowtie",1,new String[]{"CSC","C C"},Map.of("C","minecraft:black_wool","S","minecraft:string"),Map.of("aer",4,"ordo",4))));
        writes.add(save(cache,"golem_decoration_glasses",shaped("TINYGLASSES","golem_decoration_glasses",1,new String[]{"GIG"},Map.of("G","minecraft:glass","I","minecraft:iron_ingot"),Map.of("aer",4,"aqua",4))));
        writes.add(save(cache,"golem_decoration_dart",shaped("TINYDART","golem_decoration_dart",1,new String[]{"AIA","ADA","AIA"},Map.of("A","minecraft:arrow","I","minecraft:iron_ingot","D","minecraft:dispenser"),Map.of("aer",4,"ignis",4))));
        writes.add(save(cache,"golem_decoration_visor",shaped("TINYVISOR","golem_decoration_visor",1,new String[]{"IHI"},Map.of("I","minecraft:iron_ingot","H","minecraft:iron_helmet"),Map.of("terra",4,"aqua",4))));
        writes.add(save(cache,"golem_decoration_armor",shaped("TINYARMOR","golem_decoration_armor",1,new String[]{"I I","IAI"},Map.of("I","minecraft:iron_ingot","A","minecraft:iron_chestplate"),Map.of("terra",8))));
        writes.add(save(cache,"golem_decoration_mace",shaped("TINYHAMMER","golem_decoration_mace",1,new String[]{"III","III"," I "},Map.of("I","minecraft:iron_ingot"),Map.of("terra",4,"ignis",4))));
        writes.add(save(cache,"arcane_bore_base",shaped("ARCANEBORE","arcane_bore_base",1,new String[]{"WIW","IDI","WIW"},
                Map.of("W","thaumcraft:greatwood_planks","I","minecraft:iron_ingot","D","minecraft:dispenser"),Map.of("aer",10,"ordo",10))));
        writes.add(save(cache,"arcane_lamp",shaped("ARCANELAMP","arcane_lamp",1,new String[]{" S ","IAI"," N "},
                Map.of("S","minecraft:daylight_detector","I","minecraft:iron_ingot","A","thaumcraft:amber_block","N","thaumcraft:nitor"),Map.of("ignis",8,"aer",8,"aqua",4,"perditio",4))));
        writes.add(save(cache,"levitator",shaped("LEVITATOR","levitator",1,new String[]{"WEW","BNB","WAW"},
                Map.of("W","thaumcraft:greatwood_planks","E","thaumcraft:earth_shard","A","thaumcraft:air_shard","N","thaumcraft:nitor","B","minecraft:iron_ingot"),Map.of("aer",10,"terra",5))));
        writes.add(save(cache,"arcane_ear",shaped("ARCANEEAR","arcane_ear",1,new String[]{"GIG","GBG","WRW"},
                Map.of("W","thaumcraft:greatwood_planks","G","minecraft:gold_ingot","I","minecraft:iron_ingot","B","thaumcraft:zombie_brain","R","minecraft:redstone"),Map.of("aer",10,"ordo",10))));
        writes.add(save(cache,"arcane_door",shaped("WARDEDARCANA","arcane_door",1,new String[]{"TDT","DBD","TDT"},
                Map.of("B","thaumcraft:zombie_brain","T","#forge:ingots/thaumium","D","thaumcraft:greatwood_planks"),Map.of("aqua",20,"ordo",10,"terra",10,"ignis",5))));
        for (String metal:java.util.List.of("iron","gold")) {
            var recipe=shaped("WARDEDARCANA",metal+"_key",2,new String[]{"NNI","N  "},
                    Map.of("N",metal.equals("iron") ? "#forge:nuggets/iron" : "minecraft:gold_nugget","I","minecraft:"+metal+"_ingot"),Map.of("aqua",5,"ordo",5));
            if (metal.equals("gold")) recipe.addProperty("requires_warded_stone",true);
            writes.add(save(cache,metal+"_key",recipe));
        }
        var pressurePlate=shaped("WARDEDARCANA","arcane_pressure_plate",1,new String[]{" B ","TDT"},
                Map.of("B","thaumcraft:zombie_brain","T","#forge:ingots/thaumium","D","thaumcraft:greatwood_planks"),Map.of("aqua",20,"ordo",10,"terra",10,"ignis",5));
        pressurePlate.addProperty("requires_warded_stone",true);
        writes.add(save(cache,"arcane_pressure_plate",pressurePlate));
        writes.add(save(cache,"warded_glass",shaped("WARDEDARCANA","warded_glass",8,new String[]{"GGG","WBW","GGG"},
                Map.of("G","minecraft:glass","W","thaumcraft:greatwood_planks","B","thaumcraft:zombie_brain"),Map.of("aqua",5,"ordo",10,"terra",5,"ignis",5))));
        writes.add(save(cache,"advanced_alchemical_construct",shaped("ADVALCHEMYFURNACE","advanced_alchemical_construct",4,new String[]{"VAV","APA","VAV"},
                Map.of("V","thaumcraft:void_ingot","A","thaumcraft:alchemical_construct","P","thaumcraft:primordial_pearl"),Map.of("aqua",10,"ordo",30,"terra",10))));
        writes.add(save(cache,"alchemical_construct",shaped("DISTILESSENTIA","alchemical_construct",1,new String[]{"VTF","TWT","FTV"},
                Map.of("V","thaumcraft:essentia_valve","T","thaumcraft:essentia_tube","W","thaumcraft:greatwood_planks","F","thaumcraft:vis_filter"),Map.of("aqua",5,"ordo",5))));
        writes.add(save(cache,"mnemonic_matrix",shaped("THAUMATORIUM","mnemonic_matrix",1,new String[]{"IAI","ABA","IAI"},
                Map.of("I","minecraft:iron_ingot","A","thaumcraft:amber","B","thaumcraft:zombie_brain"),Map.of("ignis",5,"aqua",5,"ordo",5))));
        for (var rod:WandParts.Rod.values()) if (rod.staff() && rod!=WandParts.Rod.PRIMAL_STAFF) {
            String material=rod.tag.substring(0,rod.tag.length()-6);
            writes.add(save(cache,"staff_rod_"+material,shaped("ROD_"+rod.tag,"staff_rod_"+material,1,new String[]{"  S"," G ","G  "},
                    Map.of("S","thaumcraft:primal_charm","G","thaumcraft:wand_rod_"+material),Map.of("ordo",rod.craftCost))));
        }
        writes.add(save(cache,"vis_filter",shaped("DISTILESSENTIA","vis_filter",2,new String[]{"GWG"},Map.of("G","minecraft:gold_ingot","W","thaumcraft:silverwood_planks"),Map.of("ordo",5,"aqua",5))));
        writes.add(save(cache,"wand_rod_greatwood",shaped("ROD_greatwood","wand_rod_greatwood",1,new String[]{" G","G "},Map.of("G","thaumcraft:greatwood_log"),Map.of("perditio",WandParts.Rod.GREATWOOD.craftCost))));
        writes.add(save(cache,"essentia_resonator",shaped("TUBES","essentia_resonator",1,new String[]{"I I","INI"," S "},Map.of("I","minecraft:iron_ingot","N","minecraft:quartz","S","#forge:rods/wooden"),Map.of("aqua",5,"aer",5))));
        writes.add(save(cache,"essentia_tube",shaped("TUBES","essentia_tube",8,new String[]{" Q ","IGI"," B "},Map.of("Q","thaumcraft:quicksilver_drop","I","minecraft:iron_ingot","G","minecraft:glass","B","minecraft:gold_nugget"),Map.of("aqua",5,"ordo",5))));
        writes.add(save(cache,"mirrored_glass",shapeless("BASICARTIFACE","mirrored_glass",new String[]{"#forge:quicksilver","#forge:glass_panes/colorless"},Map.of("ignis",10,"terra",10))));
        writes.add(save(cache,"essentia_valve",shapeless("TUBES","essentia_valve",new String[]{"thaumcraft:essentia_tube","minecraft:lever"},Map.of("aqua",5,"ordo",5))));
        writes.add(save(cache,"essentia_filter",shapeless("TUBEFILTER","essentia_filter",new String[]{"thaumcraft:essentia_tube","thaumcraft:vis_filter"},Map.of("aqua",5,"ordo",16))));
        writes.add(save(cache,"essentia_restrictor",shapeless("TUBEFILTER","essentia_restrictor",new String[]{"thaumcraft:essentia_tube","#forge:stone"},Map.of("aqua",5,"terra",16))));
        writes.add(save(cache,"essentia_oneway",shapeless("TUBEFILTER","essentia_oneway",new String[]{"thaumcraft:essentia_tube","#forge:dyes/blue"},Map.of("aqua",5,"ordo",8,"perditio",8))));
        writes.add(save(cache,"essentia_buffer",shaped("CENTRIFUGE","essentia_buffer",1,new String[]{"PVP","T T","PRP"},Map.of("P","thaumcraft:empty_phial","T","thaumcraft:essentia_tube","V","thaumcraft:essentia_valve","R","thaumcraft:essentia_restrictor"),Map.of("aqua",5,"ordo",5))));
        writes.add(save(cache,"alchemy_furnace",shaped("DISTILESSENTIA","alchemy_furnace",1,new String[]{"SCS","SFS","SSS"},Map.of("S","thaumcraft:arcane_stone","C","thaumcraft:crucible","F","minecraft:furnace"),Map.of("ignis",5,"aqua",5))));
        writes.add(save(cache,"alembic",shaped("DISTILESSENTIA","alembic",1,new String[]{"FIG","IBI","I I"},Map.of("F","thaumcraft:vis_filter","I","minecraft:iron_ingot","G","minecraft:gold_ingot","B","minecraft:bucket"),Map.of("aer",5,"aqua",5))));
        writes.add(save(cache,"essentia_jar",shaped("DISTILESSENTIA","essentia_jar",1,new String[]{"GWG","G G","GGG"},Map.of("G","minecraft:glass_pane","W","#minecraft:wooden_slabs"),Map.of("aqua",1))));
        writes.add(save(cache,"void_jar",shaped("JARVOID","void_jar",1,new String[]{"O","J","P"},Map.of("O","minecraft:obsidian","J","thaumcraft:essentia_jar","P","minecraft:blaze_powder"),Map.of("aqua",5,"perditio",15))));
        writes.add(save(cache,"infusion_matrix",shaped("INFUSION","infusion_matrix",1,new String[]{"SBS","BEB","SBS"},
                Map.of("S","thaumcraft:arcane_stone","B","#thaumcraft:shards","E","minecraft:ender_pearl"),Map.of("ordo",40))));
        writes.add(save(cache,"arcane_pedestal",shaped("INFUSION","arcane_pedestal",2,new String[]{"SSS"," S ","SSS"},Map.of("S","thaumcraft:arcane_stone"),Map.of("aer",5))));
        writes.add(save(cache,"focus_primal",shaped("FOCUSPRIMAL","focus_primal",1,new String[]{"CQC","Q#Q","CQC"},
                Map.of("C","minecraft:diamond","Q","minecraft:quartz","#","thaumcraft:primal_charm"),
                Map.of("aer",25,"terra",25,"ignis",25,"aqua",25,"ordo",25,"perditio",25))));
        writes.add(save(cache,"bellows",shaped("BELLOWS","bellows",1,new String[]{"WW ","LCI","WW "},
                Map.of("W","#minecraft:planks","L","minecraft:leather","C","thaumcraft:air_shard","I","minecraft:iron_ingot"),Map.of("aer",10,"ordo",5))));
        writes.add(save(cache, "enchanted_fabric", shaped("ENCHFABRIC", "enchanted_fabric", 1, new String[]{" S ", "SCS", " S "},
                Map.of("S", "minecraft:string", "C", "#minecraft:wool"), Map.of("aer", 1, "terra", 1, "ignis", 1, "aqua", 1, "ordo", 1, "perditio", 1))));
        writes.add(save(cache, "robe_chest", shaped("ENCHFABRIC", "robe_chest", 1, new String[]{"I I", "III", "III"}, Map.of("I", "thaumcraft:enchanted_fabric"), Map.of("aer", 5))));
        writes.add(save(cache, "robe_legs", shaped("ENCHFABRIC", "robe_legs", 1, new String[]{"III", "I I", "I I"}, Map.of("I", "thaumcraft:enchanted_fabric"), Map.of("aqua", 5))));
        writes.add(save(cache, "robe_boots", shaped("ENCHFABRIC", "robe_boots", 1, new String[]{"I I", "I I"}, Map.of("I", "thaumcraft:enchanted_fabric"), Map.of("terra", 3))));
        var dye = new JsonObject(); dye.addProperty("type", "thaumcraft:armor_dye"); dye.addProperty("category", "equipment");
        // A single dispatching recipe avoids nondeterministic overlap with vanilla's broad DyeableLeatherItem matcher.
        writes.add(DataProvider.saveStable(cache, dye, output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft/recipes/armor_dye.json")));
        writes.add(save(cache, "goggles", shaped("GOGGLES", "goggles", 1, new String[]{"LGL", "L L", "TGT"},
                Map.of("L", "minecraft:leather", "G", "minecraft:gold_ingot", "T", "thaumcraft:thaumometer"),
                Map.of("aer", 5, "ignis", 5, "aqua", 5, "terra", 5, "ordo", 3, "perditio", 3))));
        writes.add(save(cache, "focus_trade", shaped("FOCUSTRADE", "focus_trade", 1, new String[]{"CQC", "Q#Q", "CQC"},
                Map.of("C", "thaumcraft:balanced_shard", "Q", "minecraft:quartz", "#", "thaumcraft:quicksilver"), Map.of("ordo", 15, "perditio", 15, "terra", 10))));
        writes.add(save(cache, "focus_excavation", shaped("FOCUSEXCAVATION", "focus_excavation", 1, new String[]{"CQC", "Q#Q", "CQC"},
                Map.of("C", "thaumcraft:earth_shard", "Q", "minecraft:quartz", "#", "#forge:gems/emerald"), Map.of("terra", 20, "perditio", 5, "ordo", 5))));
        writes.add(save(cache, "focus_fire", shaped("FOCUSFIRE", "focus_fire", 1, new String[]{"CQC", "Q#Q", "CQC"},
                Map.of("C", "thaumcraft:fire_shard", "Q", "minecraft:quartz", "#", "minecraft:fire_charge"), Map.of("ignis", 20, "perditio", 10))));
        writes.add(save(cache, "focus_frost", shaped("FOCUSFROST", "focus_frost", 1, new String[]{"CQC", "Q#Q", "CQC"},
                Map.of("C", "thaumcraft:water_shard", "Q", "minecraft:quartz", "#", "minecraft:diamond"), Map.of("aqua", 10, "ordo", 10, "perditio", 10))));
        writes.add(save(cache, "node_stabilizer", shaped("NODESTABILIZER", "node_stabilizer", 1,
                new String[]{" G ", "QPQ", "SNS"}, Map.of("G", "minecraft:gold_ingot", "Q", "minecraft:quartz_block", "P", "minecraft:piston",
                        "S", "thaumcraft:arcane_stone_bricks", "N", "thaumcraft:nitor"), Map.of("aqua", 32, "terra", 32, "ordo", 32))));
        writes.add(save(cache, "node_converter", shaped("VISPOWER", "node_converter", 1,
                new String[]{"RCR", "ISI", "RAR"}, Map.of("R", "minecraft:redstone_block", "C", "minecraft:comparator", "I", "minecraft:iron_ingot",
                        "S", "thaumcraft:node_stabilizer", "A", "thaumcraft:nitor"), Map.of("ignis", 32, "aer", 32, "perditio", 32))));
        writes.add(save(cache, "vis_relay", shaped("VISPOWER", "vis_relay", 2,
                new String[]{" I ", "ISI", " I "}, Map.of("I", "minecraft:iron_ingot", "S", "thaumcraft:balanced_shard"), Map.of("ignis", 8, "ordo", 8))));
        writes.add(save(cache, "workbench_charger", shaped("VISCHARGERELAY", "workbench_charger", 1,
                new String[]{" R ", "W W", "I I"}, Map.of("R", "thaumcraft:vis_relay", "W", "thaumcraft:wand_rod_greatwood", "I", "minecraft:iron_ingot"),
                Map.of("ignis", 16, "ordo", 16, "aer", 16))));
        writes.add(save(cache, "focal_manipulator", shaped("FOCALMANIPULATION", "focal_manipulator", 1,
                new String[]{"IQI", "SPS", "GTG"}, Map.of("I", "minecraft:iron_ingot", "Q", "thaumcraft:arcane_stone_slab", "S", "thaumcraft:arcane_stone",
                        "P", "thaumcraft:primal_charm", "G", "minecraft:gold_ingot", "T", "thaumcraft:table"),
                Map.of("aer", 32, "ignis", 32, "aqua", 32, "terra", 32, "ordo", 32, "perditio", 32))));
        writes.add(save(cache, "arcane_stone", shaped("ARCANESTONE", "arcane_stone", 9,
                new String[]{"SSS", "SCS", "SSS"}, Map.of("S", "#forge:stone", "C", "#thaumcraft:shards"), Map.of("terra", 1, "ignis", 1))));
        var shock = new JsonObject(); shock.addProperty("type", "thaumcraft:arcane"); shock.addProperty("research", "FOCUSSHOCK");
        var shockPattern = new JsonArray(); for (String line : new String[]{"CQC", "Q#Q", "CQC"}) shockPattern.add(line); shock.add("pattern", shockPattern);
        var shockKeys = new JsonObject();
        Map.of("C", tc4revival.content.ShardType.AIR.id(), "Q", "minecraft:quartz", "#", "minecraft:potato").forEach((key, item) -> {
            var ingredient = new JsonObject(); ingredient.addProperty("item", item); shockKeys.add(key, ingredient);
        });
        shock.add("key", shockKeys); var shockResult = new JsonObject(); shockResult.addProperty("item", "thaumcraft:focus_shock"); shock.add("result", shockResult);
        var shockCost = new JsonObject(); for (String aspect : new String[]{"aer", "ordo", "perditio"}) shockCost.addProperty(aspect, 10);
        shock.add("vis", shockCost); writes.add(save(cache, "focus_shock", shock));
        var pouch = new JsonObject(); pouch.addProperty("type", "thaumcraft:arcane"); pouch.addProperty("research", "FOCUSPOUCH");
        var pouchPattern = new JsonArray(); for (String line : new String[]{"LGL", "LBL", "LLL"}) pouchPattern.add(line); pouch.add("pattern", pouchPattern);
        var pouchKeys = new JsonObject();
        Map.of("L", "minecraft:leather", "G", "minecraft:gold_ingot", "B", "thaumcraft:mundane_belt").forEach((key, item) -> {
            var ingredient = new JsonObject(); ingredient.addProperty("item", item); pouchKeys.add(key, ingredient);
        });
        pouch.add("key", pouchKeys); var pouchResult = new JsonObject(); pouchResult.addProperty("item", "thaumcraft:focus_pouch"); pouch.add("result", pouchResult);
        var pouchCost = new JsonObject(); for (String aspect : new String[]{"terra", "ordo", "perditio"}) pouchCost.addProperty(aspect, 10);
        pouch.add("vis", pouchCost); writes.add(save(cache, "focus_pouch", pouch));
        var charm = new JsonObject(); charm.addProperty("type", "thaumcraft:arcane"); charm.addProperty("research", "BASICARTIFACE");
        var patternCharm = new JsonArray(); for (String line : new String[]{"123", "ISI", "456"}) patternCharm.add(line); charm.add("pattern", patternCharm);
        var keys = new JsonObject();
        for (var shard : tc4revival.content.ShardType.values()) {
            var ingredient = new JsonObject(); ingredient.addProperty("item", shard.id());
            keys.add(shard.metadata() == 6 ? "S" : Integer.toString(shard.metadata() + 1), ingredient);
        }
        var gold = new JsonObject(); gold.addProperty("item", "minecraft:gold_ingot"); keys.add("I", gold); charm.add("key", keys);
        var charmOutput = new JsonObject(); charmOutput.addProperty("item", "thaumcraft:primal_charm"); charm.add("result", charmOutput);
        var charmCost = new JsonObject(); for (String aspect : new String[]{"terra", "ignis", "aer", "aqua", "ordo", "perditio"}) charmCost.addProperty(aspect, 25);
        charm.add("vis", charmCost); writes.add(save(cache, "primal_charm", charm));
        for (String mode : new String[]{"wand", "sceptre"}) {
            var recipe = new JsonObject(); recipe.addProperty("type", "thaumcraft:arcane"); recipe.addProperty("mode", mode);
            writes.add(save(cache, "assembly_" + mode, recipe));
        }
        for (var cap : new WandParts.Cap[]{WandParts.Cap.GOLD, WandParts.Cap.COPPER, WandParts.Cap.SILVER, WandParts.Cap.THAUMIUM, WandParts.Cap.VOID}) {
            var recipe = new JsonObject(); recipe.addProperty("type", "thaumcraft:arcane"); recipe.addProperty("research", cap.research());
            var pattern = new JsonArray(); pattern.add("NNN"); pattern.add("N N"); recipe.add("pattern", pattern);
            var material = new JsonObject(); material.addProperty("tag", "forge:nuggets/" + cap.tag);
            var key = new JsonObject(); key.add("N", material); recipe.add("key", key);
            var result = new JsonObject();
            String name = "wand_cap_" + cap.tag + (cap == WandParts.Cap.SILVER || cap == WandParts.Cap.THAUMIUM || cap == WandParts.Cap.VOID ? "_inert" : "");
            result.addProperty("item", "thaumcraft:" + name); recipe.add("result", result);
            var vis = new JsonObject();
            Map<String, Integer> amounts = cap == WandParts.Cap.VOID
                    ? Map.of("perditio", 27, "ordo", 27, "ignis", 18, "aer", 18)
                    : Map.of("ordo", cap.craftCost, "ignis", cap.craftCost, "aer", cap.craftCost);
            amounts.forEach(vis::addProperty); recipe.add("vis", vis);
            if (cap == WandParts.Cap.SILVER) recipe.add("conditions", SilverRecipeConditions.present());
            writes.add(save(cache, name, recipe));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private CompletableFuture<?> save(CachedOutput cache, String name, JsonObject recipe) {
        return DataProvider.saveStable(cache, recipe, output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/recipes/arcane/" + name + ".json"));
    }
    private static JsonObject shaped(String research, String result, int count, String[] rows, Map<String, String> ingredients, Map<String, Integer> vis) {
        var recipe = new JsonObject(); recipe.addProperty("type", "thaumcraft:arcane"); recipe.addProperty("research", research);
        var pattern = new JsonArray(); for (var row : rows) pattern.add(row); recipe.add("pattern", pattern);
        var keys = new JsonObject(); ingredients.forEach((key, value) -> {
            var ingredient = new JsonObject(); ingredient.addProperty(value.startsWith("#") ? "tag" : "item", value.startsWith("#") ? value.substring(1) : value); keys.add(key, ingredient);
        }); recipe.add("key", keys);
        var output = new JsonObject(); output.addProperty("item", "thaumcraft:" + result); output.addProperty("count", count); recipe.add("result", output);
        var costs = new JsonObject(); vis.forEach(costs::addProperty); recipe.add("vis", costs); return recipe;
    }
    private static JsonObject undamaged(JsonObject recipe,String... items) {
        var required=new JsonArray(); for (var item:items) required.add(item); recipe.add("undamaged",required); return recipe;
    }
    private static JsonObject shapeless(String research,String result,String[] ingredients,Map<String,Integer> vis) {
        var recipe=new JsonObject(); recipe.addProperty("type","thaumcraft:arcane"); recipe.addProperty("mode","shapeless"); recipe.addProperty("research",research);
        var inputs=new JsonArray(); for (String value:ingredients) { var input=new JsonObject(); input.addProperty(value.startsWith("#") ? "tag" : "item",value.startsWith("#") ? value.substring(1) : value); inputs.add(input); } recipe.add("ingredients",inputs);
        var output=new JsonObject(); output.addProperty("item","thaumcraft:"+result); recipe.add("result",output);
        var costs=new JsonObject(); vis.forEach(costs::addProperty); recipe.add("vis",costs); return recipe;
    }
    @Override public String getName() { return "TC4 arcane wand assembly and casting cap recipes"; }
}
