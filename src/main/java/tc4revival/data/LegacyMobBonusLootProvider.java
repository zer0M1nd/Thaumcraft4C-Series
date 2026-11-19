package tc4revival.data;
import com.google.gson.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;
/** Reward contents are datapack loot; eligibility and the old integer roll remain death-event rules. */
public  class LegacyMobBonusLootProvider implements DataProvider {
    private final PackOutput output;
    public LegacyMobBonusLootProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new java.util.ArrayList<CompletableFuture<?>>();
        for(String item:java.util.List.of("zombie_brain","gold_coin")) {
            var entry=new JsonObject();entry.addProperty("type","minecraft:item");entry.addProperty("name","thaumcraft:"+item);
            var entries=new JsonArray();entries.add(entry);
            var pool=new JsonObject();pool.addProperty("rolls",1);pool.add("entries",entries);
            var pools=new JsonArray();pools.add(pool);
            var root=new JsonObject();root.addProperty("type","minecraft:empty");root.add("pools",pools);
            writes.add(DataProvider.saveStable(cache,root,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/legacy_mob_bonus/"+item+".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 additional mob reward contents"; }
}
