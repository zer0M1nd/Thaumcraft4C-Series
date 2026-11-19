package emtmodern.item;

import com.google.gson.*;
import emtmodern.ElectroMagicTools;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/** Inserts a weighted legacy 0..1 reward into the existing modern dungeon treasure pool. */
@Mod.EventBusSubscriber(modid="emt")
public class OneRingLoot {
    public static final ResourceLocation ENTRY_ID=new ResourceLocation("emt","one_ring");
    @SubscribeEvent public static void dungeon(LootTableLoadEvent event){
        if(!event.getName().equals(new ResourceLocation("minecraft","chests/simple_dungeon")))return;
        event.setTable(LegacyDungeonLoot.append(event.getTable(),new Entry(new LootItemCondition[0])));
    }
    public static class Entry extends LootPoolEntryContainer {
        public Entry(LootItemCondition[] conditions){super(conditions);}
        @Override public LootPoolEntryType getType(){return LegacyLootTypes.ONE_RING.get();}
        @Override public boolean expand(LootContext context,Consumer<LootPoolEntry> entries){
            if(!canRun(context))return false;
            entries.accept(new LootPoolEntry(){
                @Override public int getWeight(float luck){return RingConfig.DISABLE_ONE_RING.get()?0:RingConfig.ONE_RING_WEIGHT.get();}
                @Override public void createItemStack(Consumer<ItemStack> drops,LootContext ctx){if(ctx.getRandom().nextBoolean())drops.accept(new ItemStack(ElectroMagicTools.CONTENT.get("one_ring").get()));}
            });return true;
        }
        public static class Serializer extends LootPoolEntryContainer.Serializer<Entry> {
            @Override public void serializeCustom(JsonObject json,Entry entry,JsonSerializationContext context){}
            @Override public Entry deserializeCustom(JsonObject json,JsonDeserializationContext context,LootItemCondition[] conditions){return new Entry(conditions);}
        }
    }
}
