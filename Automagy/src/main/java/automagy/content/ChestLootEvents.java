package automagy.content;
import com.google.gson.*;
import java.util.Set;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
/** Adds weighted entries to the existing item pool; preserves its rolls and existing entries. */
@Mod.EventBusSubscriber(modid="automagy")
public final class ChestLootEvents {
 private static final Set<String> NORMAL=Set.of("abandoned_mineshaft","desert_pyramid","jungle_temple","stronghold_corridor","stronghold_library","stronghold_crossing","simple_dungeon");
 private ChestLootEvents(){}
 @SubscribeEvent public static void loaded(LootTableLoadEvent event){
    if(!event.getName().getNamespace().equals("minecraft"))return;
    String path=event.getName().getPath();boolean normal=NORMAL.contains(path.replace("chests/",""));boolean smith=path.equals("chests/village/village_weaponsmith");if(!normal&&!smith)return;
    Gson gson=Deserializers.createLootTableSerializer().create();
    var table=gson.toJsonTree(event.getTable()).getAsJsonObject();if(!table.has("pools"))return;
    for(var element:table.getAsJsonArray("pools")){
        var pool=element.getAsJsonObject();if(!pool.has("entries"))continue;var entries=pool.getAsJsonArray("entries");
        boolean itemPool=false;for(var e:entries)if(e.getAsJsonObject().has("type")&&e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")){itemPool=true;break;}
        if(!itemPool)continue;
        if(normal)add(entries,"lesser_ender_disjunction_ring",automagy.LootConfig.RING.get(),1,1);
        add(entries,"experience_phial",(normal?automagy.LootConfig.PHIAL:automagy.LootConfig.SMITH_PHIAL).get(),1,normal?3:2);
        // Rebuild only this pool through the public builder. Deserializing a full table here
        // lacks Forge's loading context; recursively loading it would also freeze it early.
        String name=pool.get("name").getAsString();
        var builder=LootPool.lootPool().name(name).setRolls(gson.fromJson(pool.get("rolls"),NumberProvider.class));
        if(pool.has("bonusRolls"))builder.setBonusRolls(gson.fromJson(pool.get("bonusRolls"),NumberProvider.class));
        if(pool.has("bonus_rolls"))builder.setBonusRolls(gson.fromJson(pool.get("bonus_rolls"),NumberProvider.class));
        for(var entry:entries)builder.add(new ExistingEntry(gson.fromJson(entry,LootPoolEntryContainer.class)));
        if(pool.has("conditions"))for(var condition:pool.getAsJsonArray("conditions"))builder.when(()->gson.fromJson(condition,LootItemCondition.class));
        if(pool.has("functions"))for(var function:pool.getAsJsonArray("functions"))builder.apply(()->gson.fromJson(function,LootItemFunction.class));
        var replacement=builder.build();
        event.getTable().removePool(name);event.getTable().addPool(replacement);return;
    }
 }
 private static class ExistingEntry extends LootPoolEntryContainer.Builder<ExistingEntry> {
    private final LootPoolEntryContainer entry;
    ExistingEntry(LootPoolEntryContainer entry){this.entry=entry;}
    @Override protected ExistingEntry getThis(){return this;}
    @Override public LootPoolEntryContainer build(){return entry;}
 }
 private static void add(JsonArray entries,String item,int weight,int min,int max){if(weight<=0)return;var e=new JsonObject();e.addProperty("type","minecraft:item");e.addProperty("name","automagy:"+item);e.addProperty("weight",weight);if(max>1){var functions=new JsonArray();var f=new JsonObject();f.addProperty("function","minecraft:set_count");var count=new JsonObject();count.addProperty("type","minecraft:uniform");count.addProperty("min",min);count.addProperty("max",max);f.add("count",count);functions.add(f);e.add("functions",functions);}entries.add(e);}
}
