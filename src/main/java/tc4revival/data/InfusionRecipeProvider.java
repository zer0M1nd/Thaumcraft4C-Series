package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Original wand-related infusion definitions. Values are data transcribed from ConfigRecipes. */
public  class InfusionRecipeProvider implements DataProvider {
    private final PackOutput output;
    public InfusionRecipeProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var recipes=new LinkedHashMap<String,JsonObject>();
        recipes.put("sanity_checker",recipe("SANITYCHECK",4,"thaumcraft:thaumometer","thaumcraft:sanity_checker",costs("cognitio",24,"sensus",24,"alienis",8),"thaumcraft:mirrored_glass","thaumcraft:zombie_brain","minecraft:diamond"));
        recipes.put("sinister_stone",recipe("SINSTONE",5,"minecraft:flint","thaumcraft:sinister_stone",costs("sensus",8,"tenebrae",8,"alienis",8,"auram",8),"thaumcraft:nitor","thaumcraft:order_shard","thaumcraft:knowledge_fragment","thaumcraft:entropy_shard"));
        recipes.put("brain_jar",recipe("JARBRAIN",4,"thaumcraft:essentia_jar","thaumcraft:brain_jar",costs("cognitio",10,"sensus",10,"exanimis",20),"thaumcraft:zombie_brain","minecraft:spider_eye","minecraft:water_bucket","minecraft:spider_eye"));
        recipes.put("item_mirror",recipe("MIRROR",1,"thaumcraft:mirrored_glass","thaumcraft:item_mirror",costs("iter",8,"tenebrae",8,"permutatio",8),"minecraft:gold_ingot","minecraft:gold_ingot","minecraft:gold_ingot","minecraft:ender_pearl"));
        recipes.put("hand_mirror",recipe("MIRRORHAND",5,"thaumcraft:item_mirror","thaumcraft:hand_mirror",costs("instrumentum",16,"iter",16),"minecraft:stick","minecraft:compass","minecraft:map"));
        recipes.put("primal_crusher",recipe("PRIMALCRUSHER",6,"thaumcraft:primordial_pearl","thaumcraft:primal_crusher",costs("perfodio",24,"instrumentum",24,"perditio",16,"vacuos",16,"telum",16,"alienis",16,"lucrum",16),"thaumcraft:primal_charm","thaumcraft:void_pickaxe","thaumcraft:void_shovel","thaumcraft:primal_charm","thaumcraft:elemental_pickaxe","thaumcraft:elemental_shovel"));
        for (int index:new int[]{1,2,4,5}) recipes.get("primal_crusher").getAsJsonArray("components").get(index).getAsJsonObject().addProperty("damage",-1);
        recipes.put("traveling_trunk",recipe("TRAVELTRUNK",3,"thaumcraft:hungry_chest","thaumcraft:traveling_trunk",costs("motus",4,"spiritus",4,"iter",4,"vacuos",16),"minecraft:iron_ingot","thaumcraft:greatwood_planks","thaumcraft:golem_wood","thaumcraft:greatwood_planks"));
        for (var material:tc4revival.content.GolemMaterial.values()) {
            String path="golem_"+material.name().toLowerCase(java.util.Locale.ROOT);
            var advanced=recipe("ADVANCEDGOLEM",3,"thaumcraft:"+path,"thaumcraft:"+path,costs("cognitio",8,"sensus",8,"victus",8),"minecraft:redstone","minecraft:glowstone_dust","minecraft:gunpowder","thaumcraft:essentia_jar","thaumcraft:zombie_brain");
            var result=new JsonObject(); result.addProperty("patch","{advanced:1b}"); advanced.add("result",result);
            recipes.put(path+"_advanced",advanced);
        }
        var alchemy=recipe("COREALCHEMY",2,"thaumcraft:golem_core_liquid","thaumcraft:golem_core_essentia",costs("praecantatio",15,"aqua",15,"motus",15),"thaumcraft:essentia_jar","minecraft:potion","minecraft:potion","minecraft:potion");
        for (int i=1;i<4;i++) alchemy.getAsJsonArray("components").get(i).getAsJsonObject().addProperty("nbt","{Potion:\"minecraft:water\"}");
        recipes.put("golem_core_essentia",alchemy);
        recipes.put("golem_core_sorting",recipe("CORESORTING",3,"thaumcraft:zombie_brain","thaumcraft:golem_core_sorting",costs("vacuos",16,"permutatio",16,"fames",16,"lucrum",16),"thaumcraft:golem_core_fill","minecraft:comparator","thaumcraft:golem_core_empty","minecraft:paper"));
        recipes.put("golem_core_lumber",recipe("CORELUMBER",2,"thaumcraft:golem_core_harvest","thaumcraft:golem_core_lumber",costs("instrumentum",16,"arbor",16,"meto",16),"thaumcraft:elemental_axe","minecraft:iron_axe","minecraft:iron_axe","minecraft:iron_axe"));
        recipes.put("golem_core_fish",recipe("COREFISHING",3,"thaumcraft:golem_core_harvest","thaumcraft:golem_core_fish",costs("aqua",16,"meto",16,"bestia",16),"minecraft:fishing_rod","minecraft:cod","minecraft:pufferfish","minecraft:salmon"));
        recipes.put("golem_core_use",recipe("COREUSE",3,"thaumcraft:golem_core_empty","thaumcraft:golem_core_use",costs("instrumentum",20,"machina",20,"humanus",20),"minecraft:comparator","minecraft:flint_and_steel","minecraft:shears","minecraft:lever"));
        recipes.put("elemental_sword",recipe("ELEMENTALSWORD",1,"thaumcraft:thaumium_sword","thaumcraft:elemental_sword",costs("aer",8,"motus",8,"potentia",8),
                "thaumcraft:air_shard","thaumcraft:air_shard","minecraft:diamond","thaumcraft:greatwood_log"));
        recipes.put("elemental_axe",recipe("ELEMENTALAXE",1,"thaumcraft:thaumium_axe","thaumcraft:elemental_axe",costs("aqua",16,"arbor",8),
                "thaumcraft:water_shard","thaumcraft:water_shard","minecraft:diamond","thaumcraft:greatwood_log"));
        recipes.put("elemental_shovel",recipe("ELEMENTALSHOVEL",1,"thaumcraft:thaumium_shovel","thaumcraft:elemental_shovel",costs("terra",16,"fabrico",8),
                "thaumcraft:earth_shard","thaumcraft:earth_shard","minecraft:diamond","thaumcraft:greatwood_log"));
        recipes.put("elemental_hoe",recipe("ELEMENTALHOE",1,"thaumcraft:thaumium_hoe","thaumcraft:elemental_hoe",costs("meto",8,"herba",8,"terra",8),
                "thaumcraft:order_shard","thaumcraft:entropy_shard","minecraft:diamond","thaumcraft:greatwood_log"));
        recipes.put("elemental_pickaxe",recipe("ELEMENTALPICK",1,"thaumcraft:thaumium_pickaxe","thaumcraft:elemental_pickaxe",costs("ignis",8,"perfodio",8,"sensus",8),
                "thaumcraft:fire_shard","thaumcraft:fire_shard","minecraft:diamond","thaumcraft:greatwood_log"));
        recipes.put("arcane_bore",recipe("ARCANEBORE",4,"minecraft:piston","thaumcraft:arcane_bore",costs("potentia",16,"perfodio",32,"machina",32,"vacuos",16,"motus",16),
                "thaumcraft:greatwood_planks","thaumcraft:greatwood_planks","minecraft:gold_ingot","minecraft:gold_ingot","minecraft:diamond_pickaxe","minecraft:diamond_shovel","thaumcraft:air_shard","thaumcraft:earth_shard"));
        recipes.put("growth_lamp",recipe("LAMPGROWTH",4,"thaumcraft:arcane_lamp","thaumcraft:growth_lamp",costs("herba",16,"lux",8,"victus",16),
                "minecraft:gold_ingot","minecraft:bone_meal","thaumcraft:earth_shard","minecraft:gold_ingot","minecraft:bone_meal","thaumcraft:earth_shard"));
        recipes.put("fertility_lamp",recipe("LAMPFERTILITY",4,"thaumcraft:arcane_lamp","thaumcraft:fertility_lamp",costs("bestia",16,"victus",16,"lux",8),
                "minecraft:gold_ingot","minecraft:wheat","thaumcraft:fire_shard","minecraft:gold_ingot","minecraft:carrot","thaumcraft:fire_shard"));
        recipes.put("hover_harness",recipe("HOVERHARNESS",6,"minecraft:leather_chestplate","thaumcraft:hover_harness",costs("volatus",32,"potentia",32,"machina",32,"iter",16),
                "thaumcraft:air_shard","thaumcraft:air_shard","thaumcraft:greatwood_planks","thaumcraft:greatwood_planks","minecraft:comparator","minecraft:gold_ingot","minecraft:gold_ingot","minecraft:iron_ingot","minecraft:iron_ingot"));
        recipes.put("hover_girdle",recipe("HOVERGIRDLE",8,"thaumcraft:mundane_belt","thaumcraft:hover_girdle",costs("volatus",16,"potentia",32,"aer",32,"iter",16),
                "thaumcraft:air_shard","minecraft:feather","minecraft:gold_ingot","thaumcraft:earth_shard","minecraft:feather","minecraft:gold_ingot"));
        recipes.put("traveller_boots",recipe("BOOTSTRAVELLER",1,"minecraft:leather_boots","thaumcraft:traveller_boots",costs("volatus",25,"iter",25),
                "thaumcraft:air_shard","thaumcraft:air_shard","thaumcraft:enchanted_fabric","thaumcraft:enchanted_fabric","minecraft:feather","#forge:raw_fishes"));
        recipes.put("void_robe_helmet",recipe("ARMORVOIDFORTRESS",6,"thaumcraft:void_helmet","thaumcraft:void_robe_helmet",costs("metallum",16,"sensus",16,"tutamen",16,"pannus",16,"praecantatio",16,"alienis",16,"vacuos",16),
                "thaumcraft:goggles","thaumcraft:enchanted_fabric","thaumcraft:enchanted_fabric","thaumcraft:salis_mundus","thaumcraft:enchanted_fabric","thaumcraft:enchanted_fabric"));
        for (String part:new String[]{"chestplate","leggings"}) {
            int amount=part.equals("chestplate")?24:20;
            recipes.put("void_robe_"+part,recipe("ARMORVOIDFORTRESS",6,"thaumcraft:void_"+part,"thaumcraft:void_robe_"+part,costs("metallum",amount,"tutamen",amount,"pannus",amount,"praecantatio",16,"alienis",16,"vacuos",amount),
                    "thaumcraft:robe_"+(part.equals("chestplate")?"chest":"legs"),"thaumcraft:void_ingot","thaumcraft:thaumium_ingot","thaumcraft:salis_mundus","thaumcraft:enchanted_fabric","minecraft:leather"));
        }
        recipes.put("fortress_helmet",recipe("ARMORFORTRESS",3,"thaumcraft:thaumium_helmet","thaumcraft:fortress_helmet",costs("metallum",24,"tutamen",16,"praecantatio",16),
                "#forge:ingots/thaumium","#forge:ingots/thaumium","minecraft:gold_ingot","minecraft:gold_ingot","minecraft:emerald"));
        recipes.put("fortress_chestplate",recipe("ARMORFORTRESS",3,"thaumcraft:thaumium_chestplate","thaumcraft:fortress_chestplate",costs("metallum",24,"tutamen",24,"praecantatio",16),
                "#forge:ingots/thaumium","#forge:ingots/thaumium","#forge:ingots/thaumium","#forge:ingots/thaumium","minecraft:gold_ingot","minecraft:leather"));
        recipes.put("fortress_leggings",recipe("ARMORFORTRESS",3,"thaumcraft:thaumium_leggings","thaumcraft:fortress_leggings",costs("metallum",24,"tutamen",20,"praecantatio",16),
                "#forge:ingots/thaumium","#forge:ingots/thaumium","#forge:ingots/thaumium","minecraft:gold_ingot","minecraft:leather"));
        recipes.put("fortress_goggles",fortressUpgrade("HELMGOGGLES",5,"{goggles:1b}",costs("sensus",32,"auram",16,"tutamen",16),"minecraft:slime_ball","thaumcraft:goggles"));
        recipes.put("fortress_mask_grinning",fortressUpgrade("MASKGRINNINGDEVIL",8,"{mask:0}",costs("cognitio",64,"sano",64,"tutamen",16),
                "minecraft:ink_sac","minecraft:iron_ingot","minecraft:leather","thaumcraft:shimmerleaf","thaumcraft:zombie_brain","minecraft:iron_ingot"));
        recipes.put("fortress_mask_angry",fortressUpgrade("MASKANGRYGHOST",8,"{mask:1}",costs("perditio",64,"mortuus",64,"tutamen",16),
                "minecraft:bone_meal","minecraft:iron_ingot","minecraft:leather","minecraft:poisonous_potato","minecraft:wither_skeleton_skull","minecraft:iron_ingot"));
        recipes.put("fortress_mask_sipping",fortressUpgrade("MASKSIPPINGFIEND",8,"{mask:2}",costs("exanimis",64,"victus",64,"tutamen",16),
                "minecraft:red_dye","minecraft:iron_ingot","minecraft:leather","minecraft:ghast_tear","minecraft:milk_bucket","minecraft:iron_ingot"));
        recipes.put("eldritch_eye",recipe("OCULUS",5,"minecraft:ender_eye","thaumcraft:eldritch_eye",
                costs("alienis",64,"vacuos",16,"tenebrae",16,"iter",16),"thaumcraft:void_seed","minecraft:gold_ingot"));
        recipes.put("essentia_mirror",recipe("MIRRORESSENTIA",2,"thaumcraft:mirrored_glass","thaumcraft:essentia_mirror",
                costs("iter",8,"aqua",8,"permutatio",8),"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:ender_pearl"));
        recipes.put("essentia_reservoir",recipe("ESSENTIARESERVOIR",6,"thaumcraft:essentia_buffer","thaumcraft:essentia_reservoir",
                costs("aqua",8,"vacuos",8,"praecantatio",8,"permutatio",8),"thaumcraft:void_ingot","thaumcraft:essentia_jar","thaumcraft:essentia_jar",
                "thaumcraft:void_ingot","thaumcraft:essentia_jar","thaumcraft:essentia_jar"));
        int silverCost=tc4revival.content.WandParts.Rod.SILVERWOOD.craftCost;
        recipes.put("wand_rod_silverwood",recipe("ROD_silverwood",5,"thaumcraft:silverwood_log","thaumcraft:wand_rod_silverwood",
                costs("aer",silverCost,"ignis",silverCost,"aqua",silverCost,"terra",silverCost,"ordo",silverCost,"perditio",silverCost,"praecantatio",silverCost),
                "thaumcraft:balanced_shard","thaumcraft:air_shard","thaumcraft:fire_shard","thaumcraft:water_shard","thaumcraft:earth_shard","thaumcraft:order_shard","thaumcraft:entropy_shard"));
        recipes.put("vis_amulet",recipe("VISAMULET",6,"thaumcraft:mundane_amulet","thaumcraft:vis_amulet",costs("auram",24,"potentia",64,"praecantatio",64,"vacuos",24),
                "thaumcraft:primal_charm","thaumcraft:mixed_crystal_cluster","thaumcraft:mixed_crystal_cluster","thaumcraft:primal_charm","thaumcraft:mixed_crystal_cluster","thaumcraft:mixed_crystal_cluster"));
        recipes.put("runic_ring",recipe("RUNICARMOR",3,"thaumcraft:mundane_ring","thaumcraft:runic_ring",costs("tutamen",10,"praecantatio",25,"potentia",25),
                "thaumcraft:primal_charm","thaumcraft:amber","thaumcraft:enchanted_fabric","thaumcraft:nitor","thaumcraft:scribing_tools"));
        recipes.put("runic_amulet",recipe("RUNICARMOR",4,"thaumcraft:mundane_amulet","thaumcraft:runic_amulet",costs("tutamen",20,"praecantatio",35,"potentia",35),
                "thaumcraft:primal_charm","thaumcraft:amber","thaumcraft:enchanted_fabric","thaumcraft:nitor","thaumcraft:nitor","thaumcraft:scribing_tools"));
        recipes.put("runic_girdle",recipe("RUNICARMOR",4,"thaumcraft:mundane_belt","thaumcraft:runic_girdle",costs("tutamen",30,"praecantatio",50,"potentia",50),
                "thaumcraft:primal_charm","thaumcraft:amber","thaumcraft:enchanted_fabric","thaumcraft:nitor","thaumcraft:nitor","thaumcraft:nitor","thaumcraft:scribing_tools"));
        recipes.put("runic_ring_charged",runicUpgrade("RUNICCHARGED",6,"runic_ring","runic_ring_charged","fire",false,"strong_swiftness",costs("tutamen",16,"praecantatio",16,"potentia",64)));
        recipes.put("runic_ring_regen",runicUpgrade("RUNICHEALING",6,"runic_ring","runic_ring_regen","water",false,"long_regeneration",costs("tutamen",16,"praecantatio",16,"aqua",32,"sano",32)));
        recipes.put("runic_amulet_emergency",runicUpgrade("RUNICEMERGENCY",7,"runic_amulet","runic_amulet_emergency","earth",false,"strong_strength",costs("tutamen",20,"praecantatio",35,"terra",32,"vacuos",32)));
        recipes.put("runic_girdle_kinetic",runicUpgrade("RUNICKINETIC",7,"runic_girdle","runic_girdle_kinetic","air",true,"strong_harming",costs("tutamen",33,"praecantatio",55,"aer",64)));
        var runic=recipe("RUNICAUGMENTATION",5,"thaumcraft:goggles","thaumcraft:goggles",costs("tutamen",16,"praecantatio",16,"potentia",32),"minecraft:diamond","thaumcraft:salis_mundus");
        runic.addProperty("mode","runic_augmentation"); recipes.put("runic_augmentation",runic);
        var silverCap=recipe("CAP_silver",4,"thaumcraft:wand_cap_silver_inert","thaumcraft:wand_cap_silver",
                costs("potentia",8,"auram",4),"thaumcraft:salis_mundus","thaumcraft:salis_mundus");
        silverCap.add("conditions",SilverRecipeConditions.present());recipes.put("wand_cap_silver",silverCap);
        recipes.put("wand_cap_thaumium",recipe("CAP_thaumium",5,"thaumcraft:wand_cap_thaumium_inert","thaumcraft:wand_cap_thaumium",
                costs("potentia",12,"auram",6),"thaumcraft:salis_mundus","thaumcraft:salis_mundus","thaumcraft:salis_mundus"));
        recipes.put("wand_cap_void",recipe("CAP_void",8,"thaumcraft:wand_cap_void_inert","thaumcraft:wand_cap_void",
                costs("potentia",18,"vacuos",18,"alienis",18,"auram",18),"thaumcraft:salis_mundus","thaumcraft:salis_mundus","thaumcraft:salis_mundus","thaumcraft:salis_mundus"));
        String[][] rods={{"obsidian","obsidian","terra","tenebrae","earth"},{"ice","ice","aqua","gelum","water"},
                {"quartz","quartz_block","ordo","vitreus","order"},{"reed","sugar_cane","aer","motus","air"},
                {"blaze","blaze_rod","ignis","bestia","fire"},{"bone","bone","perditio","exanimis","entropy"}};
        for (var rod:rods) recipes.put("wand_rod_"+rod[0],recipe("ROD_"+rod[0],3,"minecraft:"+rod[1],"thaumcraft:wand_rod_"+rod[0],
                costs(rod[2],12,"praecantatio",6,rod[3],6),"thaumcraft:balanced_shard","thaumcraft:"+rod[4]+"_shard"));
        recipes.put("staff_rod_primal",recipe("ROD_primal_staff",8,"thaumcraft:wand_rod_silverwood","thaumcraft:staff_rod_primal",
                costs("aer",32,"ignis",32,"aqua",32,"terra",32,"ordo",32,"perditio",32,"praecantatio",64),
                "thaumcraft:primal_charm","thaumcraft:wand_rod_obsidian","thaumcraft:wand_rod_ice","thaumcraft:wand_rod_quartz",
                "thaumcraft:primal_charm","thaumcraft:wand_rod_reed","thaumcraft:wand_rod_blaze","thaumcraft:wand_rod_bone"));
        recipes.put("focus_hellbat",recipe("FOCUSHELLBAT",3,"minecraft:magma_cream","thaumcraft:focus_hellbat",
                costs("ignis",25,"aer",15,"bestia",15,"perditio",25),"minecraft:quartz","thaumcraft:fire_shard","minecraft:quartz","thaumcraft:air_shard","minecraft:quartz","thaumcraft:entropy_shard"));
        recipes.put("focus_portable_hole",recipe("FOCUSPORTABLEHOLE",3,"minecraft:ender_pearl","thaumcraft:focus_portable_hole",
                costs("iter",25,"alienis",10,"permutatio",10,"perditio",25),"minecraft:quartz","thaumcraft:earth_shard","minecraft:quartz","thaumcraft:air_shard","minecraft:quartz","thaumcraft:entropy_shard"));
        recipes.put("focus_warding",recipe("FOCUSWARDING",4,"minecraft:nether_star","thaumcraft:focus_warding",
                costs("terra",25,"tutamen",25,"ordo",25,"cognitio",10),"thaumcraft:quicksilver","thaumcraft:earth_shard","minecraft:quartz","thaumcraft:order_shard",
                "thaumcraft:quicksilver","thaumcraft:earth_shard","minecraft:quartz","thaumcraft:order_shard"));
        recipes.put("wand_pedestal",recipe("WANDPED",3,"thaumcraft:arcane_pedestal","thaumcraft:wand_pedestal",
                costs("auram",10,"praecantatio",15,"permutatio",15),"#forge:ingots/gold","minecraft:diamond","thaumcraft:primal_charm","minecraft:diamond"));
        recipes.put("wand_pedestal_focus",recipe("WANDPEDFOC",4,"minecraft:comparator","thaumcraft:wand_pedestal_focus",
                costs("ordo",10,"praecantatio",15,"permutatio",10),"thaumcraft:earth_shard","thaumcraft:vis_filter","thaumcraft:earth_shard","thaumcraft:vis_filter",
                "thaumcraft:earth_shard","thaumcraft:vis_filter","thaumcraft:earth_shard","thaumcraft:vis_filter"));
        recipes.put("advanced_node_stabilizer",recipe("NODESTABILIZERADV",10,"thaumcraft:node_stabilizer","thaumcraft:advanced_node_stabilizer",
                costs("auram",32,"praecantatio",16,"ordo",16,"potentia",16),"thaumcraft:nitor","minecraft:redstone_block","thaumcraft:alumentum","minecraft:redstone_block",
                "thaumcraft:nitor","minecraft:redstone_block","thaumcraft:alumentum","minecraft:redstone_block"));
        InfusionEnchantmentRecipes.addTo(recipes);
        var writes=new ArrayList<CompletableFuture<?>>(); int order=0;
        for (var entry:recipes.entrySet()) {
            entry.getValue().addProperty("legacy_order",order++);
            writes.add(DataProvider.saveStable(cache,entry.getValue(),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/recipes/infusion/"+entry.getKey()+".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private static JsonObject recipe(String research,int instability,String center,String result,Map<String,Integer> amounts,String... components) {
        var json=new JsonObject(); json.addProperty("type","thaumcraft:infusion"); json.addProperty("research",research); json.addProperty("instability",instability);
        json.add("center",ingredient(center)); var output=new JsonObject(); output.addProperty("item",result); json.add("result",output);
        var costs=new JsonObject(); amounts.forEach(costs::addProperty); json.add("aspects",costs);
        var parts=new JsonArray(); for (String component:components) parts.add(ingredient(component)); json.add("components",parts); return json;
    }
    private static JsonObject fortressUpgrade(String research,int instability,String patch,Map<String,Integer> costs,String... components) {
        var json=recipe(research,instability,"thaumcraft:fortress_helmet","thaumcraft:fortress_helmet",costs,components);
        json.getAsJsonObject("center").addProperty("damage",-1);
        if (research.equals("HELMGOGGLES")) json.getAsJsonArray("components").get(1).getAsJsonObject().addProperty("damage",-1);
        var output=new JsonObject(); output.addProperty("patch",patch); json.add("result",output); return json;
    }
    private static JsonObject ingredient(String id) {
        var match=new JsonObject(); match.addProperty(id.startsWith("#") ? "tag" : "item",id.startsWith("#") ? id.substring(1) : id);
        var json=new JsonObject(); json.add("ingredient",match); if (id.equals("thaumcraft:scribing_tools")) json.addProperty("damage",0); return json;
    }
    private static JsonObject runicUpgrade(String research,int instability,String center,String result,String shard,boolean splash,String potion,Map<String,Integer> aspects) {
        var json=recipe(research,instability,"thaumcraft:"+center,"thaumcraft:"+result,aspects,"thaumcraft:balanced_shard","thaumcraft:"+shard+"_shard","thaumcraft:"+shard+"_shard",
                splash ? "minecraft:splash_potion" : "minecraft:potion","thaumcraft:"+shard+"_shard","thaumcraft:"+shard+"_shard");
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putString("Potion","minecraft:"+potion);
        json.getAsJsonArray("components").get(3).getAsJsonObject().addProperty("nbt",tag.toString()); return json;
    }
    private static Map<String,Integer> costs(Object... entries) {
        var result=new LinkedHashMap<String,Integer>(); for (int index=0;index<entries.length;index+=2) result.put((String)entries[index],(Integer)entries[index+1]); return result;
    }
    @Override public String getName() { return "TC4 infusion recipes"; }
}
