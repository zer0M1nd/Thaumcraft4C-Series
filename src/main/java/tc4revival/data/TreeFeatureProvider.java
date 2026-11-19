package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Reusable tree definitions. The full legacy vegetation distribution is a separate feature. */
public  class TreeFeatureProvider implements DataProvider {
    private final PackOutput output;
    public TreeFeatureProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>(); var root=output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/worldgen");
        var obelisk=new JsonObject();obelisk.addProperty("type","thaumcraft:eldritch_obelisk");
        obelisk.addProperty("biomes","#minecraft:is_overworld");obelisk.addProperty("step","surface_structures");
        obelisk.addProperty("terrain_adaptation","none");obelisk.add("spawn_overrides",new JsonObject());
        writes.add(DataProvider.saveStable(cache,obelisk,root.resolve("structure/eldritch_obelisk.json")));
        var set=new JsonObject();var entries=new JsonArray();var entry=new JsonObject();entry.addProperty("structure","thaumcraft:eldritch_obelisk");entry.addProperty("weight",1);entries.add(entry);set.add("structures",entries);
        String[] bosses={"golem","warden","crimson","taint"};
        for(int boss=0;boss<4;boss++) {
            var variant=obelisk.deepCopy();variant.addProperty("boss",boss);
            String name="eldritch_obelisk_"+bosses[boss];
            writes.add(DataProvider.saveStable(cache,variant,root.resolve("structure/"+name+".json")));
            var member=new JsonObject();member.addProperty("structure","thaumcraft:"+name);member.addProperty("weight",1);entries.add(member);
        }
        var placement=new JsonObject();placement.addProperty("type","minecraft:random_spread");placement.addProperty("spacing",32);placement.addProperty("separation",24);placement.addProperty("salt",5368321);set.add("placement",placement);
        writes.add(DataProvider.saveStable(cache,set,root.resolve("structure_set/eldritch_obelisks.json")));
        String[] ruinNames={"sinister_dungeon","hilltop_stones","obsidian_totem"},kinds={"dungeon","hilltop","totem"};int[] spacings={24,12,6}; // Issue 7: reduce surface clusters on modern terrain.
        for(int index=0;index<ruinNames.length;index++) {
            var definition=obelisk.deepCopy();definition.addProperty("type","thaumcraft:surface_ruin");definition.addProperty("kind",kinds[index]);
            writes.add(DataProvider.saveStable(cache,definition,root.resolve("structure/"+ruinNames[index]+".json")));
            var structureSet=new JsonObject();var member=new JsonObject();member.addProperty("structure","thaumcraft:"+ruinNames[index]);member.addProperty("weight",1);var members=new JsonArray();members.add(member);structureSet.add("structures",members);
            var grid=new JsonObject();grid.addProperty("type","minecraft:random_spread");grid.addProperty("spacing",spacings[index]);grid.addProperty("separation",Math.max(1,spacings[index]/2));grid.addProperty("salt",5368341+index);
            if(index>0){var exclusion=new JsonObject();exclusion.addProperty("other_set","thaumcraft:sinister_dungeon");exclusion.addProperty("chunk_count",1);grid.add("exclusion_zone",exclusion);}
            structureSet.add("placement",grid);writes.add(DataProvider.saveStable(cache,structureSet,root.resolve("structure_set/"+ruinNames[index]+".json")));
        }
        for (String name:java.util.List.of("forest_decoration","forest_ores","forest_sediments","forest_vegetation","forest_springs","forest_pumpkin","forest_reeds","forest_small_mushrooms",
                "forest_lily","forest_flowers","forest_mushroom","forest_moss","forest_understory","mana_pod","legacy_vegetation","eldritch_ring","obsidian_totem","natural_aura","taint_decoration")) {
            var configured=new JsonObject(); configured.addProperty("type","thaumcraft:"+name); configured.add("config",new JsonObject());
            writes.add(DataProvider.saveStable(cache,configured,root.resolve("configured_feature/"+name+".json")));
            var placed=new JsonObject(); placed.addProperty("feature","thaumcraft:"+name); placed.add("placement",new JsonArray());
            writes.add(DataProvider.saveStable(cache,placed,root.resolve("placed_feature/"+name+".json")));
        }
        var injection=new JsonObject(); injection.addProperty("type","forge:add_features"); injection.addProperty("biomes","#minecraft:is_overworld");
        injection.addProperty("features","thaumcraft:legacy_vegetation"); injection.addProperty("step","vegetal_decoration");
        writes.add(DataProvider.saveStable(cache,injection,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/forge/biome_modifier/legacy_vegetation.json")));
        var rings=new JsonObject(); rings.addProperty("type","forge:add_features"); rings.addProperty("biomes","#minecraft:is_overworld");
        var naturalBiomes=new JsonArray();naturalBiomes.add("#minecraft:is_overworld");naturalBiomes.add("#minecraft:is_nether");
        var naturalTag=new JsonObject();naturalTag.add("values",naturalBiomes);writes.add(DataProvider.saveStable(cache,naturalTag,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/tags/worldgen/biome/natural_aura.json")));
        // Mod dimensions need not tag their biomes as overworld/nether.
        var allBiomes=new JsonObject();allBiomes.addProperty("type","forge:any");rings.add("biomes",allBiomes);
        rings.addProperty("features","thaumcraft:natural_aura"); rings.addProperty("step","top_layer_modification");
        writes.add(DataProvider.saveStable(cache,rings,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/forge/biome_modifier/eldritch_ring.json")));
        for (String name:java.util.List.of("greatwood_plain","greatwood","greatwood_spider","silverwood","forest_silverwood","forest_oak","forest_tree")) {
            var configured=new JsonObject(); configured.addProperty("type","thaumcraft:"+name); configured.add("config",new JsonObject());
            writes.add(DataProvider.saveStable(cache,configured,root.resolve("configured_feature/"+name+".json")));
            var placed=new JsonObject(); placed.addProperty("feature","thaumcraft:"+name); var modifiers=new JsonArray();
            var heightmap=new JsonObject(); heightmap.addProperty("type","minecraft:heightmap"); heightmap.addProperty("heightmap","WORLD_SURFACE_WG"); modifiers.add(heightmap);
            placed.add("placement",modifiers); writes.add(DataProvider.saveStable(cache,placed,root.resolve("placed_feature/"+name+".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 configured tree features"; }
}

