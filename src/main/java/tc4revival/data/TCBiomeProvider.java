package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Biome identities used by world conversion; natural placement/decorators are added separately. */
public  class TCBiomeProvider implements DataProvider {
    private final PackOutput output;
    public TCBiomeProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var biome=new JsonObject();
        biome.addProperty("has_precipitation",true); biome.addProperty("temperature",.5); biome.addProperty("downfall",.5);
        var effects=new JsonObject(); effects.addProperty("grass_color",7160201); effects.addProperty("foliage_color",8154503);
        effects.addProperty("sky_color",8144127); effects.addProperty("water_color",0x320779);
        effects.addProperty("water_fog_color",0x320779); effects.addProperty("fog_color",12638463); biome.add("effects",effects);
        var spawns=new JsonObject(); var bats=new JsonArray(); var bat=new JsonObject();
        bat.addProperty("type","minecraft:bat"); bat.addProperty("weight",10); bat.addProperty("minCount",8); bat.addProperty("maxCount",8);
        bats.add(bat); spawns.add("ambient",bats); biome.add("spawners",spawns);
        biome.add("spawn_costs",new JsonObject());
        var taintCarvers=new JsonObject();var taintAir=new JsonArray();taintAir.add("minecraft:cave");taintAir.add("minecraft:canyon");taintCarvers.add("air",taintAir);biome.add("carvers",taintCarvers);
        var taintFeatures=new JsonArray();for(int step=0;step<11;step++){var entries=new JsonArray();if(step==9)entries.add("thaumcraft:taint_decoration");taintFeatures.add(entries);}biome.add("features",taintFeatures);
        var root=output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/worldgen/biome");
        return CompletableFuture.allOf(DataProvider.saveStable(cache,biome,root.resolve("tainted_land.json")),
                DataProvider.saveStable(cache,magicalForest(),root.resolve("magical_forest.json")),
                DataProvider.saveStable(cache,eerie(),root.resolve("eerie.json")),
                spookyTag(cache),forestTag(cache,"is_overworld"),forestTag(cache,"is_forest"));
    }
    private CompletableFuture<?> spookyTag(CachedOutput cache) {
        var tag=new JsonObject();tag.addProperty("replace",false);var values=new JsonArray();values.add("thaumcraft:eerie");tag.add("values",values);
        return DataProvider.saveStable(cache,tag,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("forge/tags/worldgen/biome/is_spooky.json"));
    }
    private CompletableFuture<?> forestTag(CachedOutput cache,String name) {
        var tag=new JsonObject(); tag.addProperty("replace",false);
        var values=new JsonArray(); values.add("thaumcraft:magical_forest"); tag.add("values",values);
        if(name.equals("is_overworld"))values.add("thaumcraft:tainted_land");
        return DataProvider.saveStable(cache,tag,output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("minecraft/tags/worldgen/biome/"+name+".json"));
    }
    private static JsonObject eerie() {
        var biome=new JsonObject();
        biome.addProperty("has_precipitation",true);biome.addProperty("temperature",.5);biome.addProperty("downfall",.5);
        var effects=new JsonObject();effects.addProperty("grass_color",0x404840);effects.addProperty("foliage_color",4215616);
        effects.addProperty("sky_color",0x222299);effects.addProperty("water_color",3035999);effects.addProperty("water_fog_color",3035999);effects.addProperty("fog_color",12638463);biome.add("effects",effects);
        var spawns=new JsonObject();var creatures=new JsonArray();spawn(creatures,"bat",3,1,1);spawns.add("creature",creatures);
        var monsters=new JsonArray();for(String name:java.util.List.of("spider","zombie","skeleton","creeper","slime"))spawn(monsters,name,100,4,4);
        spawn(monsters,"enderman",10,1,4);spawn(monsters,"witch",5,1,1);spawn(monsters,"witch",8,1,1);spawn(monsters,"enderman",4,1,1);spawns.add("monster",monsters);
        var ambient=new JsonArray();spawn(ambient,"bat",10,8,8);spawns.add("ambient",ambient);
        var water=new JsonArray();spawn(water,"squid",10,4,4);spawns.add("water_creature",water);
        biome.add("spawners",spawns);biome.add("spawn_costs",new JsonObject());biome.add("carvers",new JsonObject());biome.add("features",new JsonArray());
        // TC4 only creates Eerie through node conversion, never through natural biome selection.
        // Conversion leaves existing terrain and decorations untouched.
        return biome;
    }
    private static JsonObject magicalForest() {
        var biome=new JsonObject();
        biome.addProperty("has_precipitation",true); biome.addProperty("temperature",.7); biome.addProperty("downfall",.6);
        var effects=new JsonObject(); effects.addProperty("grass_color",5635969); effects.addProperty("foliage_color",6750149);
        float climate=.7F/3.0F;
        effects.addProperty("sky_color",net.minecraft.util.Mth.hsvToRgb(.62222224F-climate*.05F,.5F+climate*.1F,1.0F)); effects.addProperty("water_color",30702);
        effects.addProperty("water_fog_color",30702); effects.addProperty("fog_color",12638463); biome.add("effects",effects);
        var spawns=new JsonObject(); var creatures=new JsonArray();
        spawn(creatures,"sheep",12,4,4); spawn(creatures,"pig",10,4,4); spawn(creatures,"chicken",10,4,4);
        spawn(creatures,"cow",8,4,4); spawn(creatures,"wolf",2,1,3); spawn(creatures,"horse",2,1,3);
        spawns.add("creature",creatures);
        var monsters=new JsonArray();
        for (String name:java.util.List.of("spider","zombie","skeleton","creeper","slime")) spawn(monsters,name,100,4,4);
        spawn(monsters,"enderman",10,1,4); spawn(monsters,"witch",5,1,1);
        // These are additions to the old base spawn list, not replacements for its entries.
        spawn(monsters,"witch",3,1,1); spawn(monsters,"enderman",3,1,1); spawns.add("monster",monsters);
        var ambient=new JsonArray(); spawn(ambient,"bat",10,8,8); spawns.add("ambient",ambient);
        var water=new JsonArray(); spawn(water,"squid",10,4,4); spawns.add("water_creature",water);
        biome.add("spawners",spawns); biome.add("spawn_costs",new JsonObject());
        var carvers=new JsonObject(); var air=new JsonArray(); air.add("minecraft:cave"); air.add("minecraft:canyon");
        carvers.add("air",air); biome.add("carvers",carvers);
        var features=new JsonArray();
        for (int step=0;step<11;step++) {
            var entries=new JsonArray(); if (step==9) entries.add("thaumcraft:forest_decoration"); features.add(entries);
        }
        biome.add("features",features);
        return biome;
    }
    private static void spawn(JsonArray entries,String type,int weight,int min,int max) {
        var entry=new JsonObject(); entry.addProperty("type","minecraft:"+type); entry.addProperty("weight",weight);
        entry.addProperty("minCount",min); entry.addProperty("maxCount",max); entries.add(entry);
    }
    @Override public String getName() { return "TC4 dynamic biome definitions"; }
}
