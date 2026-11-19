package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

/** Trade facts from the two legacy managers; item families survive metadata flattening. */
public  class VillagerTradeProvider implements DataProvider {
    private final PackOutput output;
    private final Map<String,String> items=new LinkedHashMap<>();
    public VillagerTradeProvider(PackOutput output) {
        this.output=output;
        for (var value:TCItemMappingProvider.definition().getAsJsonArray("items")) {
            var row=value.getAsJsonObject(); items.put(row.get("legacy_item").getAsString()+":"+row.get("metadata").getAsInt(),row.get("item").getAsString());
        }
    }
    private JsonObject stack(String family,int min,int max,int... metadata) {
        var row=new JsonObject(); row.addProperty("family",family); row.addProperty("min",min); row.addProperty("max",max);
        var variants=new JsonArray();
        for (int meta:metadata) {
            String id=family.startsWith("minecraft:")?family:items.get(family+":"+meta);
            if (id==null) throw new IllegalStateException("Missing village trade mapping "+family+":"+meta);
            variants.add(id);
        }
        row.add("items",variants); return row;
    }
    private void add(JsonArray list,JsonObject buy,JsonObject sell) {
        var row=new JsonObject(); row.add("buy",buy); row.add("sell",sell); list.add(row);
    }
    private JsonObject legacyCandidates() {
        var root=new JsonObject(); root.addProperty("format",1);
        var wizard=new JsonArray(); var banker=new JsonArray();
        var emerald=stack("minecraft:emerald",1,1,0);
        add(wizard,stack("Thaumcraft:ItemResource",20,22,18),emerald);
        add(wizard,emerald,stack("Thaumcraft:ItemResource",1,1,9));
        add(wizard,stack("Thaumcraft:ItemResource",4,6,3),emerald);
        add(wizard,emerald,stack("Thaumcraft:ItemResource",1,1,0));
        add(wizard,stack("Thaumcraft:ItemResource",4,6,6),emerald);
        add(wizard,emerald,stack("Thaumcraft:ItemResource",1,1,1));
        add(wizard,stack("Thaumcraft:ItemNuggetChicken",24,31,0),emerald);
        add(wizard,stack("minecraft:book",4,6,0),stack("Thaumcraft:ItemResource",1,1,9));
        add(wizard,stack("Thaumcraft:ItemNuggetBeef",24,31,0),emerald);
        add(wizard,emerald,stack("Thaumcraft:ItemShard",2,3,0,1,2,3,4,5));
        add(wizard,emerald,stack("Thaumcraft:ItemManaBean",1,2,0));
        for (String item:new String[]{"ItemBathSalts","ItemRingRunic","ItemAmuletVis"})
            add(wizard,stack("minecraft:emerald",5,7,0),stack("Thaumcraft:"+item,1,1,0));
        add(wizard,stack("minecraft:emerald",5,7,0),stack("Thaumcraft:ItemBaubleBlanks",1,1,3,4,5,6,7,8));
        add(banker,stack("Thaumcraft:ItemResource",20,22,18),emerald);
        String[] goods={"arrow","white_wool","paper","book","experience_bottle","glowstone","coal","diamond","iron_ingot"};
        int[] min={2,6,3,7,16,9,2,22,6},max={3,8,4,9,20,12,3,24,8};
        for (int i=0;i<goods.length;i++) add(banker,stack("Thaumcraft:ItemResource",min[i],max[i],18),stack("minecraft:"+goods[i],1,1,0));
        add(banker,stack("Thaumcraft:ItemResource",10,12,18),stack("Thaumcraft:ItemResource",1,1,2));
        add(banker,stack("Thaumcraft:ItemResource",25,32,18),stack("minecraft:saddle",1,1,0));
        root.add("wizard",wizard); root.add("banker",banker); return root;
    }
    public JsonObject definition(String role) {
        var source=legacyCandidates().getAsJsonArray(role);
        int[] levels=role.equals("wizard")?new int[]{1,3,1,2,2,2,1,3,1,3,4,4,5,5,5}:new int[]{4,1,2,1,2,4,3,1,5,2,3,5};
        var root=new JsonObject();root.addProperty("format",1);var groups=new JsonObject();root.add("levels",groups);
        for(int i=1;i<=5;i++)groups.add(""+i,new JsonArray());
        for(int i=0;i<source.size();i++) {
            var trade=source.get(i).deepCopy().getAsJsonObject();
            trade.getAsJsonObject("buy").remove("family");trade.getAsJsonObject("sell").remove("family");
            int level=levels[i];trade.addProperty("max_uses",level<3?16:level<5?12:3);
            trade.addProperty("xp",new int[]{0,2,5,10,15,30}[level]);
            trade.addProperty("price_multiplier",level<4?.05f:.2f);
            groups.getAsJsonArray(""+level).add(trade);
        }
        return root;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new java.util.ArrayList<CompletableFuture<?>>();
        for(String role:new String[]{"wizard","banker"})writes.add(DataProvider.saveStable(cache,definition(role),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/villager_trades/"+role+".json")));
        var tag=new JsonObject();tag.addProperty("replace",false);var values=new JsonArray();values.add("thaumcraft:wizard");values.add("thaumcraft:banker");tag.add("values",values);
        writes.add(DataProvider.saveStable(cache,tag,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft/tags/point_of_interest_type/acquirable_job_site.json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "Data-pack villager levels and workplace tags"; }
}
