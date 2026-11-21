package emtmodern.item;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/** Preserves the existing dungeon pools while adding a legacy weighted entry to the treasure pool. */
public class LegacyDungeonLoot {
    public static LootTable append(LootTable original,LootPoolEntryContainer added){
        var gson=Deserializers.createLootTableSerializer().create();
        var data=gson.toJsonTree(original).getAsJsonObject();
        var pools=data.getAsJsonArray("pools");if(pools==null||pools.isEmpty())return original;
        var table=LootTable.lootTable();
        if(data.has("type")){var params=LootContextParamSets.get(new ResourceLocation(data.get("type").getAsString()));if(params!=null)table.setParamSet(params);}
        if(data.has("random_sequence"))table.setRandomSequence(new ResourceLocation(data.get("random_sequence").getAsString()));
        if(data.has("functions"))for(var f:gson.fromJson(data.get("functions"),LootItemFunction[].class))table.apply(()->f);
        // LootPool's Gson adapter requires Forge's private load context, which has already ended at
        // LootTableLoadEvent. Builders deserialize only entries/providers and never re-enter that adapter.
        for(int i=0;i<pools.size();i++){
            var json=pools.get(i).getAsJsonObject();
            var pool=LootPool.lootPool().name(json.has("name")?json.get("name").getAsString():"custom#"+i)
                .setRolls(gson.fromJson(json.get("rolls"),NumberProvider.class));
            if(json.has("bonus_rolls"))pool.setBonusRolls(gson.fromJson(json.get("bonus_rolls"),NumberProvider.class));
            for(var entry:gson.fromJson(json.get("entries"),LootPoolEntryContainer[].class))pool.add(new ExistingEntry(entry));
            if(i==0)pool.add(new ExistingEntry(added));
            if(json.has("conditions"))for(var c:gson.fromJson(json.get("conditions"),LootItemCondition[].class))pool.when(()->c);
            if(json.has("functions"))for(var f:gson.fromJson(json.get("functions"),LootItemFunction[].class))pool.apply(()->f);
            table.withPool(pool);
        }
        return table.build();
    }
    public static class ExistingEntry extends LootPoolEntryContainer.Builder<ExistingEntry>{
        protected LootPoolEntryContainer entry;
        public ExistingEntry(LootPoolEntryContainer entry){this.entry=entry;}
        @Override protected ExistingEntry getThis(){return this;}
        @Override public LootPoolEntryContainer build(){return entry;}
    }
}
