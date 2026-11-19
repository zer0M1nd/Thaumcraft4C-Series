package tc4revival.data;

import com.google.gson.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

/** Original AIFish pools; stack counts, weights and damage factors are independent. */
public  class GolemFishingProvider implements DataProvider {
    private final PackOutput output;
    public GolemFishingProvider(PackOutput output) { this.output=output; }
    public static JsonObject definition() {
        var root=new JsonObject(); var junk=new JsonArray(); var treasure=new JsonArray(); var fish=new JsonArray();
        add(junk,"leather_boots",1,10,.9f,false); add(junk,"leather",1,10,0,false); add(junk,"bone",1,10,0,false);
        add(junk,"potion",1,10,0,false).addProperty("nbt","{Potion:\"minecraft:water\"}");
        add(junk,"string",1,5,0,false); add(junk,"fishing_rod",1,2,.9f,false); add(junk,"bowl",1,10,0,false);
        add(junk,"stick",1,5,0,false); add(junk,"ink_sac",10,5,0,false); add(junk,"tripwire_hook",1,10,0,false); add(junk,"rotten_flesh",1,10,0,false);
        add(treasure,"lily_pad",1,1,0,false); add(treasure,"name_tag",1,1,0,false); add(treasure,"saddle",1,1,0,false);
        add(treasure,"bow",1,1,.25f,true); add(treasure,"fishing_rod",1,1,.25f,true); add(treasure,"book",1,1,0,true);
        add(fish,"cod",1,60,0,false); add(fish,"salmon",1,25,0,false); add(fish,"tropical_fish",1,2,0,false); add(fish,"pufferfish",1,13,0,false);
        root.add("junk",junk); root.add("treasure",treasure); root.add("fish",fish); return root;
    }
    private static JsonObject add(JsonArray pool,String item,int count,int weight,float damage,boolean enchant) {
        var row=new JsonObject(); var stack=new JsonObject(); stack.addProperty("item","minecraft:"+item); stack.addProperty("count",count);
        row.add("stack",stack); row.addProperty("weight",weight); row.addProperty("damage",damage); row.addProperty("enchant",enchant); pool.add(row); return stack;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveStable(cache,definition(),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/golem_fishing/default.json"));
    }
    @Override public String getName() { return "TC4 golem fishing pools"; }
}
