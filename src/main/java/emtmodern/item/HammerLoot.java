package emtmodern.item;

import com.google.gson.*;
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

@Mod.EventBusSubscriber(modid="emt")
public class HammerLoot {
    public static final ResourceLocation ID=new ResourceLocation("emt","tainted_mjolnir");
    @SubscribeEvent public static void dungeon(LootTableLoadEvent event){if(event.getName().equals(new ResourceLocation("minecraft","chests/simple_dungeon")))event.setTable(LegacyDungeonLoot.append(event.getTable(),new Entry(new LootItemCondition[0])));}
    public static class Entry extends LootPoolEntryContainer{
        public Entry(LootItemCondition[] conditions){super(conditions);}
        @Override public LootPoolEntryType getType(){return LegacyLootTypes.HAMMER.get();}
        @Override public boolean expand(LootContext context,Consumer<LootPoolEntry> entries){
            if(!canRun(context))return false;entries.accept(new LootPoolEntry(){
                @Override public int getWeight(float luck){return HammerConfig.DISABLE_MJOLNIR.get()?0:HammerConfig.BROKEN_WEIGHT.get();}
                @Override public void createItemStack(Consumer<ItemStack> output,LootContext ctx){if(ctx.getRandom().nextBoolean())output.accept(new ItemStack(emtmodern.ElectroMagicTools.CONTENT.get("tainted_mjolnir").get()));}
            });return true;
        }
        public static class Serializer extends LootPoolEntryContainer.Serializer<Entry>{
            @Override public void serializeCustom(JsonObject json,Entry entry,JsonSerializationContext context){}
            @Override public Entry deserializeCustom(JsonObject json,JsonDeserializationContext context,LootItemCondition[] conditions){return new Entry(conditions);}
        }
    }
}
