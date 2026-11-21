package tc4revival.data;
import com.google.gson.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;
public  class VillageResourcesProvider implements DataProvider {
    private final PackOutput output;
    public VillageResourcesProvider(PackOutput output){this.output=output;}
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new java.util.ArrayList<CompletableFuture<?>>();
        try {
            for(String biome:new String[]{"plains","desert","savanna"}) {
                String path="data/minecraft/worldgen/structure/village_"+biome+".json";
                try(var stream=java.util.Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path));var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)) {
                    var original=JsonParser.parseReader(reader).getAsJsonObject();
                    if (original.has("type")&&original.get("type").getAsString().equals("thaumcraft:legacy_village")) original=original.getAsJsonObject("delegate");
                    original.remove("type");
                    var wrapper=new JsonObject();wrapper.addProperty("type","thaumcraft:legacy_village");wrapper.add("delegate",original);
                    writes.add(DataProvider.saveStable(cache,wrapper,output.getOutputFolder().resolve(path)));
                }
            }
            var root=new JsonObject();var entries=new JsonArray();root.add("entries",entries);
            String[] items={"minecraft:glowstone_dust","minecraft:glass_bottle","minecraft:gold_nugget","minecraft:fire_charge","minecraft:skeleton_skull","thaumcraft:knowledge_fragment","thaumcraft:alumentum","thaumcraft:nitor","thaumcraft:thaumium_ingot","thaumcraft:thaumonomicon"};
            int[] max={3,5,3,1,1,3,1,1,2,1},weights={3,10,5,5,3,20,5,5,5,20};
            for(int i=0;i<items.length;i++){var row=new JsonObject();var stack=new JsonObject();stack.addProperty("item",items[i]);row.add("stack",stack);row.addProperty("min",1);row.addProperty("max",max[i]);row.addProperty("weight",weights[i]);entries.add(row);}
            writes.add(DataProvider.saveStable(cache,root,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/legacy_chest_loot/wizard_tower.json")));
            writes.add(DataProvider.saveStable(cache,ModernLootProvider.chest(root,ModernLootProvider.uniform(4,8)),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/chests/wizard_tower.json")));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        }catch(java.io.IOException e){return CompletableFuture.failedFuture(e);}
    }
    @Override public String getName(){return "Legacy village building integration and wizard chest";}
}
