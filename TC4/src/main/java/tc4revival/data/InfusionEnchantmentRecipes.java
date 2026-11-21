package tc4revival.data;

import com.google.gson.*;
import java.util.Map;

/** The 24 ConfigRecipes infusion enchantments; XP is fixed to the 1.7.10 runtime baseline. */
 class InfusionEnchantmentRecipes {
    static final java.util.List<String> MODERN = java.util.List.of("depth_strider", "frost_walker", "binding_curse", "soul_speed", "swift_sneak", "sweeping", "mending", "vanishing_curse", "luck_of_the_sea", "lure", "loyalty", "impaling", "riptide", "channeling", "multishot", "quick_charge", "piercing");
    private InfusionEnchantmentRecipes() {}
    static void addTo(Map<String,JsonObject> recipes) {
        add(recipes,"thaumcraft:repair",4,6,costs("praecantatio",8,"fabrico",10,"ordo",10),"minecraft:anvil");
        add(recipes,"thaumcraft:haste",3,5,costs("praecantatio",4,"iter",8,"volatus",8),"thaumcraft:nitor");
        add(recipes,"minecraft:protection",1,1,costs("praecantatio",4,"tutamen",8),"minecraft:iron_ingot");
        add(recipes,"minecraft:fire_protection",1,3,costs("praecantatio",4,"tutamen",4,"ignis",4),"minecraft:iron_ingot","minecraft:magma_cream");
        add(recipes,"minecraft:blast_protection",1,1,costs("praecantatio",4,"tutamen",4,"perditio",4),"minecraft:iron_ingot","minecraft:gunpowder");
        add(recipes,"minecraft:projectile_protection",1,1,costs("praecantatio",4,"tutamen",4,"volatus",4),"minecraft:iron_ingot","minecraft:arrow");
        add(recipes,"minecraft:feather_falling",1,1,costs("praecantatio",4,"aer",4,"volatus",4),"minecraft:feather");
        add(recipes,"minecraft:respiration",2,3,costs("praecantatio",4,"aer",8,"aqua",8),"minecraft:sugar_cane");
        add(recipes,"minecraft:aqua_affinity",2,1,costs("praecantatio",4,"motus",8,"aqua",8),"minecraft:sugar_cane","minecraft:slime_ball");
        add(recipes,"minecraft:thorns",2,3,costs("praecantatio",4,"telum",8,"herba",8),"minecraft:dead_bush","minecraft:quartz");
        add(recipes,"minecraft:sharpness",2,1,costs("praecantatio",4,"telum",8),"minecraft:iron_sword");
        add(recipes,"minecraft:smite",2,1,costs("praecantatio",4,"telum",4,"exanimis",4),"minecraft:iron_sword","minecraft:glowstone_dust");
        add(recipes,"minecraft:bane_of_arthropods",2,1,costs("praecantatio",4,"telum",4,"bestia",4),"minecraft:iron_sword","thaumcraft:amber");
        add(recipes,"minecraft:knockback",1,1,costs("praecantatio",4,"telum",3,"motus",3),"minecraft:piston");
        add(recipes,"minecraft:fire_aspect",3,3,costs("praecantatio",4,"telum",4,"ignis",8),"minecraft:iron_sword","minecraft:blaze_powder");
        add(recipes,"minecraft:looting",3,5,costs("praecantatio",4,"telum",4,"lucrum",8),"minecraft:iron_sword","minecraft:diamond");
        add(recipes,"minecraft:efficiency",2,1,costs("praecantatio",4,"instrumentum",4,"ordo",4),"minecraft:iron_pickaxe");
        add(recipes,"minecraft:silk_touch",5,5,costs("praecantatio",16,"instrumentum",16,"ordo",16,"meto",16,"perfodio",16),"minecraft:iron_pickaxe","minecraft:cobweb");
        add(recipes,"minecraft:unbreaking",2,1,costs("praecantatio",4,"instrumentum",4,"ordo",8),"minecraft:iron_pickaxe","minecraft:obsidian");
        add(recipes,"minecraft:fortune",3,5,costs("praecantatio",4,"instrumentum",4,"lucrum",8),"minecraft:iron_pickaxe","minecraft:diamond");
        add(recipes,"minecraft:power",2,1,costs("praecantatio",4,"telum",8),"minecraft:bow");
        add(recipes,"minecraft:punch",2,4,costs("praecantatio",4,"telum",3,"motus",3),"minecraft:piston");
        add(recipes,"minecraft:flame",3,6,costs("praecantatio",4,"telum",4,"ignis",8),"minecraft:bow","minecraft:blaze_powder");
        add(recipes,"minecraft:infinity",5,6,costs("praecantatio",8,"telum",16,"vacuos",16,"permutatio",16),"minecraft:bow","minecraft:arrow");
        add(recipes,"minecraft:depth_strider",3,4,costs("praecantatio",8,"aqua",16,"iter",8),"minecraft:prismarine_shard","minecraft:leather_boots");
        add(recipes,"minecraft:frost_walker",4,6,costs("praecantatio",12,"gelum",16,"iter",8),"minecraft:packed_ice","minecraft:leather_boots");
        add(recipes,"minecraft:binding_curse",4,5,costs("praecantatio",12,"vinculum",16,"alienis",8),"minecraft:chain","minecraft:carved_pumpkin");
        add(recipes,"minecraft:soul_speed",5,8,costs("praecantatio",16,"spiritus",24,"iter",16),"minecraft:soul_sand","minecraft:golden_boots");
        add(recipes,"minecraft:swift_sneak",5,8,costs("praecantatio",16,"motus",24,"sensus",16),"minecraft:echo_shard","minecraft:sculk");
        add(recipes,"minecraft:sweeping",3,4,costs("praecantatio",8,"telum",16,"aer",8),"minecraft:iron_sword","minecraft:feather");
        add(recipes,"minecraft:mending",6,10,costs("praecantatio",32,"cognitio",32,"fabrico",24),"minecraft:experience_bottle","minecraft:anvil","minecraft:diamond");
        add(recipes,"minecraft:vanishing_curse",4,5,costs("praecantatio",12,"vacuos",16,"mortuus",8),"minecraft:ender_pearl","minecraft:bone");
        add(recipes,"minecraft:luck_of_the_sea",3,5,costs("praecantatio",8,"aqua",12,"lucrum",12),"minecraft:fishing_rod","minecraft:nautilus_shell");
        add(recipes,"minecraft:lure",2,3,costs("praecantatio",8,"aqua",8,"bestia",8),"minecraft:fishing_rod","minecraft:tropical_fish");
        add(recipes,"minecraft:loyalty",3,4,costs("praecantatio",8,"vinculum",12,"motus",12),"minecraft:ender_pearl","minecraft:lead");
        add(recipes,"minecraft:impaling",3,4,costs("praecantatio",8,"telum",16,"aqua",8),"minecraft:prismarine_shard","minecraft:iron_sword");
        add(recipes,"minecraft:riptide",4,6,costs("praecantatio",16,"aqua",24,"volatus",16),"minecraft:nautilus_shell","minecraft:feather");
        add(recipes,"minecraft:channeling",5,8,costs("praecantatio",16,"potentia",32,"tempestas",16),"minecraft:lightning_rod","minecraft:heart_of_the_sea");
        add(recipes,"minecraft:multishot",4,6,costs("praecantatio",12,"telum",16,"permutatio",16),"minecraft:crossbow","minecraft:arrow","minecraft:arrow");
        add(recipes,"minecraft:quick_charge",3,4,costs("praecantatio",8,"motus",16,"instrumentum",8),"minecraft:crossbow","minecraft:redstone");
        add(recipes,"minecraft:piercing",3,4,costs("praecantatio",8,"telum",16,"perfodio",8),"minecraft:arrow","minecraft:quartz");
    }
    private static JsonObject costs(Object... pairs) {
        var result=new JsonObject();for(int i=0;i<pairs.length;i+=2)result.addProperty((String)pairs[i],(Integer)pairs[i+1]);return result;
    }
    private static void add(Map<String,JsonObject> recipes,String enchantment,int instability,int xp,JsonObject costs,String... items) {
        var result=new JsonObject();result.addProperty("type","thaumcraft:infusion");result.addProperty("research","INFUSIONENCHANTMENT");
        result.addProperty("enchantment",enchantment);result.addProperty("base_xp",xp);result.addProperty("instability",instability);result.add("aspects",costs);
        var components=new JsonArray();for(var item:items)components.add(ingredient(item));components.add(ingredient("thaumcraft:salis_mundus"));result.add("components",components);
        recipes.put("enchantment/"+enchantment.substring(enchantment.indexOf(':')+1),result);
    }
    private static JsonObject ingredient(String id) {
        var value=new JsonObject();value.addProperty("item",id);var result=new JsonObject();result.add("ingredient",value);return result;
    }
}
