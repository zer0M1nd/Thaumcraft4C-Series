package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/** Base-mod smelting bonuses from ConfigRecipes; optional integration metals are outside this release. */
public  class InfernalBonusProvider implements DataProvider {
    private final PackOutput output;
    public InfernalBonusProvider(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>(); int order=0;
        for (String metal:new String[]{"gold","iron","cinnabar","copper"}) {
            String item=switch(metal) { case "gold","iron"->"minecraft:"+metal+"_nugget";
                case "cinnabar"->"thaumcraft:quicksilver_drop"; default->"thaumcraft:copper_nugget"; };
            // The old lookup tries exact item/metadata before consulting the ore dictionary.
            writes.add(save(cache,"ore_"+metal,definition(100+order,"#forge:ores/"+metal,item,0)));
            writes.add(save(cache,"cluster_"+metal,definition(order++,"thaumcraft:"+metal+"_cluster",item,0)));
        }
        for (String meat:new String[]{"chicken","beef","pork"})
            writes.add(save(cache,meat,definition(order++,"minecraft:"+(meat.equals("pork") ? "porkchop" : meat),"thaumcraft:"+meat+"_nugget",0)));
        // ThaumcraftApi.addSmeltingBonus discards the supplied count even for food.
        var fish=definition(order,"minecraft:cod","thaumcraft:fish_nugget",0); var fishItems=new JsonArray();
        // The old wildcard fish metadata covers these four items; only smeltable inputs reach bonus output.
        for (String kind:new String[]{"cod","salmon","tropical_fish","pufferfish"}) { var entry=new JsonObject(); entry.addProperty("item","minecraft:"+kind); fishItems.add(entry); }
        fish.add("ingredient",fishItems); writes.add(save(cache,"fish",fish));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private JsonObject definition(int order,String input,String result,int count) {
        var json=new JsonObject(); json.addProperty("type","thaumcraft:infernal_bonus"); json.addProperty("legacy_order",order);
        var ingredient=new JsonObject(); ingredient.addProperty(input.startsWith("#") ? "tag" : "item",input.startsWith("#") ? input.substring(1) : input);
        json.add("ingredient",ingredient); var target=new JsonObject(); target.addProperty("item",result); json.add("result",target);
        json.addProperty("base_count",count); return json;
    }
    private CompletableFuture<?> save(CachedOutput cache,String name,JsonObject json) {
        return DataProvider.saveStable(cache,json,output.createPathProvider(PackOutput.Target.DATA_PACK,"recipes")
                .json(new ResourceLocation("thaumcraft","infernal_bonus/"+name)));
    }
    @Override public String getName() { return "TC4 infernal furnace bonuses"; }
}
