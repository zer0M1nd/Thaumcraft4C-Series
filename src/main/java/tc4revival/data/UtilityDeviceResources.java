package tc4revival.data;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

public  class UtilityDeviceResources implements DataProvider {
    public static final String[][] IDENTITIES={{"levitator","blockLifter","0"},{"arcane_ear","blockWoodenDevice","1"}};
    private final PackOutput output;
    public UtilityDeviceResources(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>(); var assets=output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft");
        var lifter=model("lifterside","liftertop","arcaneearbottom"); var base=new JsonArray(); base.add(cube(0,0,0,16,16,16,"side","top","bottom",-1)); lifter.add("elements",base);
        writes.add(DataProvider.saveStable(cache,lifter,assets.resolve("models/block/levitator.json")));
        var inventory=lifter.deepCopy(); inventory.getAsJsonObject("textures").addProperty("glow","thaumcraft:block/animatedglow");
        inventory.getAsJsonArray("elements").add(cube(.16,14.4,.16,15.84,15.84,15.84,"glow","glow","glow",0));
        inventory.getAsJsonArray("elements").add(cube(.16,1.6,.16,15.84,14.4,15.84,"glow","glow","glow",1));
        writes.add(DataProvider.saveStable(cache,inventory,assets.resolve("models/item/levitator.json")));
        writes.add(DataProvider.saveStable(cache,states(Map.of("","levitator")),assets.resolve("blockstates/levitator.json")));
        for (boolean powered:new boolean[]{false,true}) {
            String suffix=powered?"on":"off"; var ear=model("arcaneearside"+suffix,"arcaneeartop"+suffix,"arcaneearbottom");
            ear.getAsJsonObject("textures").addProperty("bell_side","thaumcraft:block/arcaneearbellside"); ear.getAsJsonObject("textures").addProperty("bell_top","thaumcraft:block/arcaneearbelltop");
            var parts=new JsonArray(); parts.add(cube(0,0,0,16,3,16,"side","top","bottom",-1)); parts.add(cube(4,3,4,12,16,12,"side","top","bottom",-1));
            for (double[] bounds:new double[][]{{4,8,1,12,16,3},{5,8,3,11,15,4},{1,8,4,3,16,12},{3,8,5,4,15,11},
                    {4,8,13,12,16,15},{5,8,12,11,15,13},{13,8,4,15,16,12},{12,8,5,13,15,11}})
                parts.add(cube(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5],"bell_side","bell_top","bell_top",-1));
            ear.add("elements",parts); writes.add(DataProvider.saveStable(cache,ear,assets.resolve("models/block/arcane_ear_"+suffix+".json")));
        }
        writes.add(DataProvider.saveStable(cache,states(Map.of("powered=false","arcane_ear_off","powered=true","arcane_ear_on")),assets.resolve("blockstates/arcane_ear.json")));
        var item=new JsonObject(); item.addProperty("parent","thaumcraft:block/arcane_ear_on"); writes.add(DataProvider.saveStable(cache,item,assets.resolve("models/item/arcane_ear.json")));
        for (var identity:IDENTITIES) writes.add(DataProvider.saveStable(cache,loot(identity[0]),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/blocks/"+identity[0]+".json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    static JsonObject model(String side,String top,String bottom) {
        var model=new JsonObject(); model.addProperty("parent","minecraft:block/block"); model.addProperty("render_type","minecraft:cutout"); var textures=new JsonObject();
        textures.addProperty("side","thaumcraft:block/"+side); textures.addProperty("top","thaumcraft:block/"+top); textures.addProperty("bottom","thaumcraft:block/"+bottom); textures.addProperty("particle","thaumcraft:block/"+side); model.add("textures",textures); return model;
    }
    static JsonObject cube(double x,double y,double z,double xx,double yy,double zz,String side,String top,String bottom,int tint) {
        var cube=new JsonObject(); cube.add("from",vector(x,y,z)); cube.add("to",vector(xx,yy,zz)); var faces=new JsonObject();
        for (String face:List.of("down","up","north","south","west","east")) { var data=new JsonObject(); data.addProperty("texture","#"+(face.equals("down")?bottom:face.equals("up")?top:side)); if (tint>=0) data.addProperty("tintindex",tint); faces.add(face,data); }
        cube.add("faces",faces); return cube;
    }
    private static JsonArray vector(double... values) { var result=new JsonArray(); for (double value:values) result.add(value); return result; }
    static JsonObject states(Map<String,String> entries) { var result=new JsonObject(); var variants=new JsonObject(); entries.forEach((key,model)->{ var entry=new JsonObject(); entry.addProperty("model","thaumcraft:block/"+model); variants.add(key,entry); }); result.add("variants",variants); return result; }
    static JsonObject loot(String name) {
        var result=new JsonObject(); result.addProperty("type","minecraft:block"); var pool=new JsonObject(); pool.addProperty("rolls",1); var entry=new JsonObject(); entry.addProperty("type","minecraft:item"); entry.addProperty("name","thaumcraft:"+name);
        var entries=new JsonArray(); entries.add(entry); pool.add("entries",entries); var condition=new JsonObject(); condition.addProperty("condition","minecraft:survives_explosion"); var conditions=new JsonArray(); conditions.add(condition); pool.add("conditions",conditions); var pools=new JsonArray(); pools.add(pool); result.add("pools",pools); return result;
    }
    @Override public String getName() { return "TC4 arcane ear and levitator resources"; }
}
