package tc4revival.data;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Recipes for currently implemented catalysts/results; later content adds its own original entries. */
public  class CrucibleRecipeProvider implements DataProvider {
    private final PackOutput output;
    public CrucibleRecipeProvider(PackOutput output) { this.output = output; }
    private record Entry(String research, String output, int count, String catalyst, int order, Map<String, Integer> aspects) {}
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var recipes = new LinkedHashMap<String, Entry>();
        recipes.put("taint_bottle",new Entry("BOTTLETAINT","thaumcraft:taint_bottle",1,"thaumcraft:essentia_phial",33,Map.of("vitium",8,"praecantatio",8)));
        recipes.put("ethereal_bloom",new Entry("ETHEREALBLOOM","thaumcraft:ethereal_bloom",1,"thaumcraft:shimmerleaf",29,Map.of("praecantatio",16,"herba",16,"sano",16,"vitium",8)));
        recipes.put("liquid_death",new Entry("LIQUIDDEATH","thaumcraft:liquid_death_bucket",1,"minecraft:bucket",30,Map.of("mortuus",32,"venenum",32,"perditio",32)));
        recipes.put("golem_thaumium",new Entry("GOLEMTHAUMIUM","thaumcraft:golem_thaumium",1,"thaumcraft:thaumium_block",41,Map.of("humanus",8,"motus",8,"spiritus",8)));
        String[] golems={"straw","wood","tallow","clay","flesh","stone","iron"};
        String[] catalysts={"minecraft:hay_block","thaumcraft:greatwood_log","thaumcraft:tallow_block","minecraft:bricks","thaumcraft:flesh_block","minecraft:stone_bricks","minecraft:iron_block"};
        for (int i=0;i<golems.length;i++) {
            int amount=i==2 || i==4?8:4;
            recipes.put("golem_"+golems[i],new Entry("GOLEM"+golems[i].toUpperCase(java.util.Locale.ROOT),"thaumcraft:golem_"+golems[i],1,catalysts[i],34+i,Map.of("humanus",amount,"motus",amount,"spiritus",amount)));
        }
        String[][] cores={{"gather","lucrum","terra"},{"fill","fames","vacuos"},{"empty","lucrum","vacuos"},{"harvest","messis","meto"},{"guard","telum","vinculum"},{"butcher","corpus","bestia"},{"liquid","aqua","vacuos"}};
        for (int i=0;i<cores.length;i++) {
            var core=cores[i]; recipes.put("golem_core_"+core[0],new Entry("CORE"+core[0].toUpperCase(java.util.Locale.ROOT),"thaumcraft:golem_core_"+core[0],1,"thaumcraft:golem_core_"+(core[0].equals("butcher")?"guard":"blank"),42+i,Map.of(core[1],5,core[2],5)));
        }
        recipes.put("bath_salts",new Entry("BATHSALTS","thaumcraft:bath_salts",1,"thaumcraft:salis_mundus",32,Map.of("cognitio",6,"auram",6,"ordo",6,"sano",6)));
        recipes.put("sanity_soap",new Entry("SANESOAP","thaumcraft:sanity_soap",1,"thaumcraft:tallow_block",33,Map.of("cognitio",16,"alienis",16,"ordo",16,"sano",16)));
        recipes.put("alumentum",new Entry("ALUMENTUM","thaumcraft:alumentum",1,"#minecraft:coals",6,Map.of("potentia",3,"ignis",3,"perditio",3)));
        String[] primals = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
        String[] shards = {"air", "fire", "water", "earth", "order", "entropy"};
        for (int i = 0; i < 6; i++) {
            var costs = new LinkedHashMap<String, Integer>(); for (int j = 0; j < 6; j++) if (i != j) costs.put(primals[j], 2);
            recipes.put("balanced_shard_from_" + shards[i], new Entry("CRUCIBLE", "thaumcraft:balanced_shard", 1, "thaumcraft:" + shards[i] + "_shard", i, costs));
        }
        recipes.put("nitor", new Entry("NITOR", "thaumcraft:nitor", 1, "#forge:dusts/glowstone", 7, Map.of("potentia", 3, "ignis", 3, "lux", 3)));
        recipes.put("thaumium", new Entry("THAUMIUM", "thaumcraft:thaumium_ingot", 1, "minecraft:iron_ingot", 8, Map.of("praecantatio", 4)));
        recipes.put("void_metal", new Entry("VOIDMETAL", "thaumcraft:void_ingot", 1, "thaumcraft:void_seed", 9, Map.of("metallum", 8)));
        recipes.put("void_seed", new Entry("VOIDMETAL", "thaumcraft:void_seed", 1, "minecraft:wheat_seeds", 10, Map.of("tenebrae", 8, "vacuos", 8, "alienis", 2)));
        recipes.put("tallow", new Entry("TALLOW", "thaumcraft:magic_tallow", 1, "minecraft:rotten_flesh", 11, Map.of("praecantatio", 2)));
        String[] outputs = {"gunpowder", "slime_ball", "clay_ball", "glowstone_dust", "ink_sac"};
        var duplicateCosts = java.util.List.of(Map.of("ignis", 4, "perditio", 4), Map.of("aqua", 2, "victus", 2), Map.of("aqua", 1, "terra", 2),
                Map.of("lux", 3, "sensus", 1), Map.of("aqua", 2, "sensus", 2));
        for (int i = 0; i < outputs.length; i++) recipes.put("duplicate_" + outputs[i], new Entry("ALCHEMICALDUPLICATION", "minecraft:" + outputs[i], 2,
                i == 3 ? "#forge:dusts/glowstone" : "minecraft:" + outputs[i], 12 + i, duplicateCosts.get(i)));
        recipes.put("cobweb", new Entry("ALCHEMICALMANUFACTURE", "minecraft:cobweb", 1, "minecraft:string", 17, Map.of("vinculum", 2, "pannus", 2)));
        recipes.put("mossy_cobblestone", new Entry("ALCHEMICALMANUFACTURE", "minecraft:mossy_cobblestone", 1, "minecraft:cobblestone", 18, Map.of("herba", 2, "praecantatio", 1)));
        recipes.put("ice", new Entry("ALCHEMICALMANUFACTURE", "minecraft:ice", 1, "minecraft:snow_block", 19, Map.of("ordo", 1, "gelum", 1)));
        recipes.put("cracked_bricks", new Entry("ENTROPICPROCESSING", "minecraft:cracked_stone_bricks", 1, "minecraft:stone_bricks", 20, Map.of("perditio", 2)));
        recipes.put("bone_meal", new Entry("ENTROPICPROCESSING", "minecraft:bone_meal", 4, "minecraft:bone", 21, Map.of("perditio", 1)));
        for(var waxed:net.minecraft.world.item.HoneycombItem.WAXABLES.get().entrySet()) {
            var bare=waxed.getKey();var coated=waxed.getValue();
            String bareId=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bare).toString();
            String coatedId=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(coated).toString();
            if(!bareId.startsWith("minecraft:")||!coatedId.startsWith("minecraft:"))continue;
            recipes.put("dewax_"+coatedId.substring(coatedId.indexOf(':')+1),new Entry("ENTROPICPROCESSING",bareId,1,coatedId,100,Map.of("aqua",1)));
            net.minecraft.world.level.block.WeatheringCopper.getNext(bare).ifPresent(next->{
                String nextId=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(next).toString();
                String nextWaxed=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(net.minecraft.world.item.HoneycombItem.WAXABLES.get().get(next)).toString();
                recipes.put("oxidize_"+bareId.substring(bareId.indexOf(':')+1),new Entry("ENTROPICPROCESSING",nextId,1,bareId,101,Map.of("perditio",1)));
                recipes.put("oxidize_"+coatedId.substring(coatedId.indexOf(':')+1),new Entry("ENTROPICPROCESSING",nextWaxed,1,coatedId,101,Map.of("perditio",1)));
            });
        }
        int index = 22;
        for (String metal : java.util.List.of("iron", "gold", "copper")) {
            String research="PURE"+metal.toUpperCase(java.util.Locale.ROOT),result="thaumcraft:"+metal+"_cluster";
            recipes.put("pure_"+metal,new Entry(research,result,2,"#forge:ores/"+metal,index++,Map.of("metallum",2,"ordo",2)));
            recipes.put("pure_"+metal+"_from_raw",new Entry(research,result,1,"#forge:raw_materials/"+metal,index++,Map.of("metallum",1,"ordo",1)));
        }
        recipes.put("transmute_iron", new Entry("TRANSIRON", "minecraft:iron_nugget", 3, "#forge:nuggets/iron", 29, Map.of("metallum", 2)));
        recipes.put("transmute_gold", new Entry("TRANSGOLD", "minecraft:gold_nugget", 3, "minecraft:gold_nugget", 30, Map.of("metallum", 2, "lucrum", 1)));
        recipes.put("transmute_copper", new Entry("TRANSCOPPER", "thaumcraft:copper_nugget", 3, "#forge:nuggets/copper", 31, Map.of("metallum", 2, "permutatio", 1)));
        var writes = new ArrayList<CompletableFuture<?>>();
        recipes.forEach((name, recipe) -> {
            var json = new JsonObject(); json.addProperty("type", "thaumcraft:crucible"); json.addProperty("research", recipe.research);
            if (name.startsWith("transmute_")) json.addProperty("nugget_transmutation",true);
            json.addProperty("legacy_order", recipe.order); var catalyst = new JsonObject();
            catalyst.addProperty(recipe.catalyst.startsWith("#") ? "tag" : "item", recipe.catalyst.startsWith("#") ? recipe.catalyst.substring(1) : recipe.catalyst); json.add("catalyst", catalyst);
            var result = new JsonObject(); result.addProperty("item", recipe.output); result.addProperty("count", recipe.count); json.add("result", result);
            var aspects = new JsonObject(); recipe.aspects.forEach(aspects::addProperty); json.add("aspects", aspects);
            writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/recipes/crucible/" + name + ".json")));
        });
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 crucible recipes"; }
}
