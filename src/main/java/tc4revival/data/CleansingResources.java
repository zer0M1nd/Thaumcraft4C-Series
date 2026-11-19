package tc4revival.data;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

public  class CleansingResources implements DataProvider {
    public static final String[][] IDENTITIES={{"bath_salts","ItemBathSalts","0"},{"sanity_soap","ItemSanitySoap","0"},
            {"tallow_block","blockCosmeticSolid","5"},{"purifying_water","blockFluidPure","0"}};
    private final PackOutput output;
    public CleansingResources(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>();
        var assets=output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft");
        for (var entry:new String[][]{{"bath_salts","bath_salts"},{"sanity_soap","soap"}}) {
            var model=new JsonObject(); model.addProperty("parent","minecraft:item/generated"); var textures=new JsonObject();
            textures.addProperty("layer0","thaumcraft:item/"+entry[1]); model.add("textures",textures);
            writes.add(DataProvider.saveStable(cache,model,assets.resolve("models/item/"+entry[0]+".json")));
        }
        for (String name:List.of("tallow_block","purifying_water")) {
            var model=new JsonObject(); var textures=new JsonObject();
            if (name.equals("tallow_block")) {
                model.addProperty("parent","minecraft:block/cube_column"); textures.addProperty("side","thaumcraft:block/tallowblock"); textures.addProperty("end","thaumcraft:block/tallowblock_top");
            } else textures.addProperty("particle","thaumcraft:block/fluidpure");
            model.add("textures",textures); writes.add(DataProvider.saveStable(cache,model,assets.resolve("models/block/"+name+".json")));
            var states=new JsonObject(); var variants=new JsonObject(); var entry=new JsonObject(); entry.addProperty("model","thaumcraft:block/"+name); variants.add("",entry); states.add("variants",variants);
            writes.add(DataProvider.saveStable(cache,states,assets.resolve("blockstates/"+name+".json")));
            var item=new JsonObject(); item.addProperty("parent","thaumcraft:block/"+name);
            if (name.equals("purifying_water")) { item.addProperty("parent","minecraft:block/cube_all"); var water=new JsonObject(); water.addProperty("all","thaumcraft:block/fluidpure"); item.add("textures",water); item.addProperty("render_type","minecraft:translucent"); }
            writes.add(DataProvider.saveStable(cache,item,assets.resolve("models/item/"+name+".json")));
        }
        var loot=new JsonObject(); loot.addProperty("type","minecraft:block"); var pools=new JsonArray(); var pool=new JsonObject(); pool.addProperty("rolls",1);
        var entries=new JsonArray(); var entry=new JsonObject(); entry.addProperty("type","minecraft:item"); entry.addProperty("name","thaumcraft:tallow_block"); entries.add(entry); pool.add("entries",entries);
        var conditions=new JsonArray(); var survives=new JsonObject(); survives.addProperty("condition","minecraft:survives_explosion"); conditions.add(survives); pool.add("conditions",conditions); pools.add(pool); loot.add("pools",pools);
        writes.add(DataProvider.saveStable(cache,loot,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/blocks/tallow_block.json")));
        var waterTag=new JsonObject(); var fluids=new JsonArray(); fluids.add("thaumcraft:purifying_water"); fluids.add("thaumcraft:flowing_purifying_water"); waterTag.add("values",fluids);
        writes.add(DataProvider.saveStable(cache,waterTag,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft/tags/fluids/water.json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 cleansing resources"; }
}
