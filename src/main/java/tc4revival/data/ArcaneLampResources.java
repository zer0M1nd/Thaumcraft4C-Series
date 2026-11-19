package tc4revival.data;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

public  class ArcaneLampResources implements DataProvider {
    public static final String[][] IDENTITIES={{"arcane_lamp","blockMetalDevice","7"},{"growth_lamp","blockMetalDevice","8"},{"fertility_lamp","blockMetalDevice","13"},{"amber_block","blockCosmeticOpaque","0"}};
    private final PackOutput output;
    public ArcaneLampResources(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>();
        var assets=output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft");
        var data=output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft");
        for (int i=0;i<3;i++) {
            String name=IDENTITIES[i][0],prefix=i==0?"lamp":i==1?"lamp_grow":"lamp_fert";
            for (boolean active:new boolean[]{false,true}) {
                String suffix=active || i==0?"":"_off";
                var model=UtilityDeviceResources.model(prefix+"_side"+suffix,prefix+"_top"+suffix,prefix+"_top"+suffix);
                var elements=new JsonArray(); elements.add(UtilityDeviceResources.cube(4,2,4,12,14,12,"side","top","bottom",-1)); model.add("elements",elements);
                writes.add(DataProvider.saveStable(cache,model,assets.resolve("models/block/"+name+(active?"_on":"_off")+".json")));
            }
            writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("active=true",name+"_on","active=false",name+"_off")),assets.resolve("blockstates/"+name+".json")));
            var item=new JsonObject(); item.addProperty("parent","thaumcraft:block/"+name+"_on");
            writes.add(DataProvider.saveStable(cache,item,assets.resolve("models/item/"+name+".json")));
        }
        var amber=UtilityDeviceResources.model("amberblock","amberblock_top","amberblock_top");
        var elements=new JsonArray(); elements.add(UtilityDeviceResources.cube(0,0,0,16,16,16,"side","top","bottom",-1)); amber.add("elements",elements);
        writes.add(DataProvider.saveStable(cache,amber,assets.resolve("models/block/amber_block.json")));
        var item=new JsonObject(); item.addProperty("parent","thaumcraft:block/amber_block");
        writes.add(DataProvider.saveStable(cache,item,assets.resolve("models/item/amber_block.json")));
        writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("","amber_block")),assets.resolve("blockstates/amber_block.json")));
        // The invisible light has no item or drops, but still has an empty baked model.
        writes.add(DataProvider.saveStable(cache,new JsonObject(),assets.resolve("models/block/lamp_air.json")));
        writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("","lamp_air")),assets.resolve("blockstates/lamp_air.json")));
        for (var identity:IDENTITIES) writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.loot(identity[0]),data.resolve("loot_tables/blocks/"+identity[0]+".json")));
        writes.add(DataProvider.saveStable(cache,amberRecipe(false),data.resolve("recipes/amber_block.json")));
        writes.add(DataProvider.saveStable(cache,amberRecipe(true),data.resolve("recipes/amber_from_block.json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private static JsonObject amberRecipe(boolean unpack) {
        var recipe=new JsonObject(); recipe.addProperty("type","minecraft:crafting_shaped"); recipe.addProperty("category","misc");
        var pattern=new JsonArray(); pattern.add(unpack?"#":"##"); if (!unpack) pattern.add("##"); recipe.add("pattern",pattern);
        var ingredient=new JsonObject(); ingredient.addProperty("item","thaumcraft:"+(unpack?"amber_block":"amber")); var key=new JsonObject(); key.add("#",ingredient); recipe.add("key",key);
        var result=new JsonObject(); result.addProperty("item","thaumcraft:"+(unpack?"amber":"amber_block")); result.addProperty("count",unpack?4:1); recipe.add("result",result); return recipe;
    }
    @Override public String getName() { return "TC4 arcane lamps and amber block"; }
}
