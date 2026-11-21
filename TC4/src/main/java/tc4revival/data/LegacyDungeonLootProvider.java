package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Vanilla 1.7.10 dungeon entries, Forge's book, then the TC4 additions in registration order. */
public  class LegacyDungeonLootProvider implements DataProvider {
    private final PackOutput output;
    public LegacyDungeonLootProvider(PackOutput output) { this.output=output; }
    public static JsonObject definition() {
        var entries=new JsonArray();
        add(entries,"minecraft:saddle",1,1,10);
        add(entries,"minecraft:iron_ingot",1,4,10);
        add(entries,"minecraft:bread",1,1,10);
        add(entries,"minecraft:wheat",1,4,10);
        add(entries,"minecraft:gunpowder",1,4,10);
        add(entries,"minecraft:string",1,4,10);
        add(entries,"minecraft:bucket",1,1,10);
        add(entries,"minecraft:golden_apple",1,1,1);
        add(entries,"minecraft:redstone",1,4,10);
        add(entries,"minecraft:music_disc_13",1,1,10);
        add(entries,"minecraft:music_disc_cat",1,1,10);
        add(entries,"minecraft:name_tag",1,1,10);
        add(entries,"minecraft:golden_horse_armor",1,1,2);
        add(entries,"minecraft:iron_horse_armor",1,1,5);
        add(entries,"minecraft:diamond_horse_armor",1,1,1);
        add(entries,"minecraft:enchanted_book",1,1,1).addProperty("random_book",true);
        for (String item:new String[]{"common_loot_bag","thaumium_ingot","amber"}) add(entries,"thaumcraft:"+item,1,3,5);
        for (String item:new String[]{"uncommon_loot_bag","mundane_amulet","mundane_ring","mundane_belt","knowledge_fragment"}) add(entries,"thaumcraft:"+item,1,2,4);
        for (String item:new String[]{"rare_loot_bag","thaumonomicon","thaumium_sword","thaumium_pickaxe","thaumium_axe","thaumium_hoe","runic_ring_lesser"}) add(entries,"thaumcraft:"+item,1,1,1);
        for (var type:tc4revival.content.ElementalRingType.values()) add(entries,"thaumcraft:"+type.path(),1,1,1);
        add(entries,"thaumcraft:lesser_vis_amulet",1,1,1).getAsJsonObject("stack").addProperty("precharged_amulet",true);
        var root=new JsonObject(); root.add("entries",entries); return root;
    }
    private static JsonObject add(JsonArray entries,String item,int minimum,int maximum,int weight) {
        var stack=new JsonObject(); stack.addProperty("item",item); var row=new JsonObject(); row.add("stack",stack);
        row.addProperty("min",minimum); row.addProperty("max",maximum); row.addProperty("weight",weight); entries.add(row); return row;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) { return DataProvider.saveStable(cache,definition(),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/legacy_chest_loot/dungeon.json")); }
    @Override public String getName() { return "TC4 old dungeon chest pool"; }
}
