package tc4revival.data;

import com.google.gson.*;
import net.minecraft.data.*;
import java.util.concurrent.CompletableFuture;

/** Ordered TC4 profession pools. Duplicate rows intentionally retain their selection weight. */
public  class PechTradeProvider implements DataProvider {
    private final PackOutput output;
    public PechTradeProvider(PackOutput output) { this.output=output; }
    public static JsonObject definition() {
        var root=new JsonObject(); var pools=new JsonArray(); for (int i=0;i<3;i++) pools.add(new JsonArray());
        var forage=pools.get(0).getAsJsonArray(); var mage=pools.get(1).getAsJsonArray(); var archer=pools.get(2).getAsJsonArray();
        for (var pool:java.util.List.of(forage,mage,archer)) add(pool,1,"thaumcraft:mana_bean");
        for (String metal:new String[]{"iron","gold","cinnabar","copper"}) add(forage,1,"thaumcraft:"+metal+"_cluster");
        add(forage,2,"minecraft:blaze_rod"); add(forage,2,"thaumcraft:greatwood_sapling"); potion(forage,2,"strength"); potion(forage,2,"swiftness");
        add(forage,3,"minecraft:experience_bottle"); add(forage,3,"thaumcraft:knowledge_fragment"); add(forage,3,"minecraft:golden_apple"); potion(forage,3,"long_strength"); potion(forage,3,"long_night_vision");
        add(forage,5,"minecraft:enchanted_golden_apple"); add(forage,4,"thaumcraft:thaumium_pickaxe"); add(forage,5,"thaumcraft:silverwood_sapling"); add(forage,5,"thaumcraft:silverwood_sapling");
        for (var shard:tc4revival.content.ShardType.values()) if (shard.metadata()<6) add(mage,1,"thaumcraft:"+shard.path());
        add(mage,1,"thaumcraft:knowledge_fragment"); add(mage,2,"thaumcraft:knowledge_fragment"); potion(mage,2,"regeneration"); potion(mage,2,"long_healing");
        book(mage,3,"thaumcraft:haste"); add(mage,3,"minecraft:golden_apple"); potion(mage,3,"strong_regeneration"); potion(mage,3,"strong_healing");
        for (var shard:tc4revival.content.ShardType.values()) add(mage,4,"thaumcraft:"+tc4revival.content.CrystalClusterBlock.path(shard));
        add(mage,5,"minecraft:enchanted_golden_apple"); book(mage,5,"thaumcraft:repair"); add(mage,5,"thaumcraft:focus_pouch"); add(mage,5,"thaumcraft:focus_pech"); add(mage,5,"thaumcraft:lesser_vis_amulet");
        for (int i=0;i<15;i++) add(archer,1,"thaumcraft:tallow_candle_"+i);
        add(archer,2,"minecraft:ghast_tear"); potion(archer,2,"swiftness"); potion(archer,2,"strength"); book(archer,2,"minecraft:power");
        add(archer,3,"minecraft:experience_bottle"); add(archer,3,"thaumcraft:knowledge_fragment"); potion(archer,3,"long_invisibility"); potion(archer,3,"strong_regeneration"); add(archer,3,"minecraft:golden_apple");
        add(archer,5,"minecraft:enchanted_golden_apple"); add(archer,4,"thaumcraft:thaumium_boots"); add(archer,5,"thaumcraft:runic_ring_lesser"); book(archer,5,"minecraft:flame"); book(archer,5,"minecraft:infinity");
        root.add("professions",pools); return root;
    }
    private static JsonObject add(JsonArray pool,int value,String item) { var row=new JsonObject(); row.addProperty("value",value); var stack=new JsonObject(); stack.addProperty("item",item); row.add("stack",stack); pool.add(row); return stack; }
    private static void potion(JsonArray pool,int value,String potion) {
        // Old 8261 has the extended bit on an instant-healing potion: retain its ordinary healing effect.
        add(pool,value,"minecraft:potion").addProperty("nbt","{Potion:\"minecraft:"+(potion.equals("long_healing") ? "healing" : potion)+"\"}");
    }
    private static void book(JsonArray pool,int value,String enchantment) { add(pool,value,"minecraft:enchanted_book").addProperty("nbt","{StoredEnchantments:[{id:\""+enchantment+"\",lvl:1s}]}"); }
    @Override public CompletableFuture<?> run(CachedOutput cache) { return DataProvider.saveStable(cache,definition(),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/pech_trades/default.json")); }
    @Override public String getName() { return "TC4 Pech trading pools"; }
}
