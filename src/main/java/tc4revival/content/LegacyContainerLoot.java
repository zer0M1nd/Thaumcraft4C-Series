package tc4revival.content;

import com.google.gson.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** One old container roll: draw count and item selection share the world's random stream. */
public  class LegacyContainerLoot extends LootPoolSingletonContainer {
    private static final DeferredRegister<LootPoolEntryType> TYPES=DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE,"thaumcraft");
    private static final RegistryObject<LootPoolEntryType> TYPE=TYPES.register("legacy_container",()->new LootPoolEntryType(new Serializer()));
    private final int rarity;
    private LegacyContainerLoot(int rarity,int weight,int quality,LootItemCondition[] conditions,LootItemFunction[] functions) {
        super(weight,quality,conditions,functions);
        if(rarity<0||rarity>2)throw new IllegalArgumentException("Invalid container rarity");this.rarity=rarity;
    }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static LootPoolSingletonContainer.Builder<?> entry(int rarity) {
        return simpleBuilder((weight,quality,conditions,functions)->new LegacyContainerLoot(rarity,weight,quality,conditions,functions));
    }
    @Override public LootPoolEntryType getType() { return TYPE.get(); }
    @Override protected void createItemStack(Consumer<ItemStack> output,LootContext context) {
        var random=context.getRandom();int count=1+rarity+random.nextInt(3);
        var table=context.getResolver().getLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:bags/roll_"+rarity));
        for(int roll=0;roll<count;roll++) table.getRandomItemsRaw(context,output);
    }
    public static  class Serializer extends LootPoolSingletonContainer.Serializer<LegacyContainerLoot> {
        @Override public void serializeCustom(JsonObject json,LegacyContainerLoot value,JsonSerializationContext context) {
            super.serializeCustom(json,value,context);json.addProperty("rarity",value.rarity);
        }
        @Override protected LegacyContainerLoot deserialize(JsonObject json,JsonDeserializationContext context,int weight,int quality,LootItemCondition[] conditions,LootItemFunction[] functions) {
            return new LegacyContainerLoot(GsonHelper.getAsInt(json,"rarity"),weight,quality,conditions,functions);
        }
    }
}
