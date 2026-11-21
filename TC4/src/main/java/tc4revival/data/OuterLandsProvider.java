package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

/** Vanilla's empty flat generator supplies terrain; persistent maze plans place rooms on the server thread. */
public  class OuterLandsProvider implements DataProvider {
    private final PackOutput output;
    public OuterLandsProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var type=new JsonObject();
        type.addProperty("ultrawarm",false); type.addProperty("natural",false); type.addProperty("coordinate_scale",1);
        type.addProperty("has_skylight",false); type.addProperty("has_ceiling",false); type.addProperty("has_raids",false); type.addProperty("ambient_light",0);
        type.addProperty("fixed_time",6000); type.addProperty("piglin_safe",false); type.addProperty("bed_works",false); type.addProperty("respawn_anchor_works",false);
        type.addProperty("min_y",0); type.addProperty("height",128); type.addProperty("logical_height",128);
        type.addProperty("infiniburn","#minecraft:infiniburn_overworld"); type.addProperty("effects","thaumcraft:outer_lands");
        type.addProperty("monster_spawn_block_light_limit",7);
        var light=new JsonObject(); light.addProperty("type","minecraft:uniform"); var bounds=new JsonObject(); bounds.addProperty("min_inclusive",0); bounds.addProperty("max_inclusive",7); light.add("value",bounds); type.add("monster_spawn_light_level",light);
        var dimension=new JsonObject(); dimension.addProperty("type","thaumcraft:outer_lands");
        var generator=new JsonObject(); generator.addProperty("type","minecraft:flat"); var settings=new JsonObject();
        settings.addProperty("biome","thaumcraft:outer_lands"); settings.addProperty("lakes",false); settings.addProperty("features",false);
        settings.add("layers",new JsonArray()); settings.add("structure_overrides",new JsonArray()); generator.add("settings",settings); dimension.add("generator",generator);
        var biome=new JsonObject(); biome.addProperty("has_precipitation",false); biome.addProperty("temperature",.5); biome.addProperty("downfall",.5);
        var effects=new JsonObject(); effects.addProperty("sky_color",0); effects.addProperty("fog_color",0xA080A0);
        effects.addProperty("water_color",4159204); effects.addProperty("water_fog_color",329011); biome.add("effects",effects);
        var monsters=new JsonArray();
        for (String id:new String[]{"inhabited_zombie","eldritch_guardian"}) {
            var spawn=new JsonObject(); spawn.addProperty("type","thaumcraft:"+id); spawn.addProperty("weight",1); spawn.addProperty("minCount",1); spawn.addProperty("maxCount",1); monsters.add(spawn);
        }
        var spawners=new JsonObject(); spawners.add("monster",monsters); biome.add("spawners",spawners);
        biome.add("spawn_costs",new JsonObject()); biome.add("carvers",new JsonObject()); biome.add("features",new JsonArray());
        var root=output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft");
        return CompletableFuture.allOf(DataProvider.saveStable(cache,type,root.resolve("dimension_type/outer_lands.json")),
                DataProvider.saveStable(cache,dimension,root.resolve("dimension/outer_lands.json")),DataProvider.saveStable(cache,biome,root.resolve("worldgen/biome/outer_lands.json")));
    }
    @Override public String getName() { return "TC4 Outer Lands dimension and biome"; }
}
