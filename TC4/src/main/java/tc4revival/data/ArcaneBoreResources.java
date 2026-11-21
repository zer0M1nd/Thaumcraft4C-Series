package tc4revival.data;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

public  class ArcaneBoreResources implements DataProvider {
    public static final String[][] IDENTITIES={{"arcane_bore_base","blockWoodenDevice","4"},{"arcane_bore","blockWoodenDevice","5"}};
    private final PackOutput output;
    public ArcaneBoreResources(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>();
        var assets=output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft");
        var pick=new JsonObject(); pick.addProperty("parent","minecraft:item/handheld"); var pickTextures=new JsonObject(); pickTextures.addProperty("layer0","thaumcraft:item/elementalpick"); pick.add("textures",pickTextures);
        writes.add(DataProvider.saveStable(cache,pick,assets.resolve("models/item/elemental_pickaxe.json")));
        var hoe=pick.deepCopy(); hoe.getAsJsonObject("textures").addProperty("layer0","thaumcraft:item/elementalhoe");
        writes.add(DataProvider.saveStable(cache,hoe,assets.resolve("models/item/elemental_hoe.json")));
        for (String tool:List.of("axe","shovel","sword")) {
            var model=pick.deepCopy(); model.getAsJsonObject("textures").addProperty("layer0","thaumcraft:item/elemental"+tool);
            writes.add(DataProvider.saveStable(cache,model,assets.resolve("models/item/elemental_"+tool+".json")));
        }
        var modifier=new JsonObject(); modifier.addProperty("type","thaumcraft:elemental_mining"); modifier.add("conditions",new JsonArray());
        writes.add(DataProvider.saveStable(cache,modifier,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_modifiers/elemental_mining.json")));
        var modifiers=new JsonObject(); modifiers.addProperty("replace",false); var entries=new JsonArray(); entries.add("thaumcraft:elemental_mining"); modifiers.add("entries",entries);
        writes.add(DataProvider.saveStable(cache,modifiers,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("forge/loot_modifiers/global_loot_modifiers.json")));
        for (var identity:IDENTITIES) {
            String name=identity[0]; var model=new JsonObject(); var textures=new JsonObject(); textures.addProperty("particle","thaumcraft:blocks/woodplain"); model.add("textures",textures);
            writes.add(DataProvider.saveStable(cache,model,assets.resolve("models/block/"+name+".json")));
            writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("",name)),assets.resolve("blockstates/"+name+".json")));
            var item=new JsonObject(); item.addProperty("parent","builtin/entity"); item.add("textures",textures.deepCopy());
            var display=new JsonObject(); display.add("gui",transform(new double[]{30,225,0},new double[]{0,0,0},.625));
            display.add("ground",transform(new double[]{0,0,0},new double[]{0,3,0},.25));
            display.add("fixed",transform(new double[]{0,0,0},new double[]{0,0,0},.5));
            display.add("thirdperson_righthand",transform(new double[]{75,45,0},new double[]{0,2.5,0},.375));
            display.add("thirdperson_lefthand",transform(new double[]{75,45,0},new double[]{0,2.5,0},.375));
            display.add("firstperson_righthand",transform(new double[]{0,45,0},new double[]{0,0,0},.4));
            display.add("firstperson_lefthand",transform(new double[]{0,225,0},new double[]{0,0,0},.4)); item.add("display",display);
            writes.add(DataProvider.saveStable(cache,item,assets.resolve("models/item/"+name+".json")));
            writes.add(DataProvider.saveStable(cache,UtilityDeviceResources.loot(name),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/blocks/"+name+".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private static JsonObject transform(double[] rotation,double[] translation,double scale) {
        var result=new JsonObject(); result.add("rotation",vector(rotation)); result.add("translation",vector(translation)); result.add("scale",vector(new double[]{scale,scale,scale})); return result;
    }
    private static JsonArray vector(double[] values) { var result=new JsonArray(); for (double value:values) result.add(value); return result; }
    @Override public String getName() { return "TC4 arcane bore resources"; }
}
