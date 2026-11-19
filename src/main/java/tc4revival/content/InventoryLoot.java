package tc4revival.content;

import com.google.gson.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.*;

/** Inventory contents cannot be expressed as a fixed item entry. This entry does not choose rewards. */
public  class InventoryLoot extends LootPoolSingletonContainer {
    private static final DeferredRegister<LootPoolEntryType> TYPES=DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE,"thaumcraft");
    private static final RegistryObject<LootPoolEntryType> TYPE=TYPES.register("inventory",()->new LootPoolEntryType(new Serializer()));
    private final String source;
    private final float chance;
    private final JsonObject result;
    private final int divisor,minimum;
    private InventoryLoot(String source,float chance,JsonObject result,int divisor,int minimum,int weight,int quality,LootItemCondition[] conditions,LootItemFunction[] functions) { super(weight,quality,conditions,functions);this.source=source;this.chance=chance;this.result=result;this.divisor=Math.max(1,divisor);this.minimum=Math.max(1,minimum); }
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { TYPES.register(bus); }
    public LootPoolEntryType getType() { return TYPE.get(); }
    protected void createItemStack(Consumer<ItemStack> output,LootContext context) {
        var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if(source.equals("pech_pack") && entity instanceof PechEntity pech) for(int slot=0;slot<pech.pack.getContainerSize();slot++) {
            var stack=pech.pack.getItem(slot);
            if(!stack.isEmpty() && context.getRandom().nextFloat()<chance)output.accept(stack.copy());
        }
        if(source.equals("entity_aspects") && entity instanceof net.minecraft.world.entity.LivingEntity living) {
            var aspects=tc4revival.aspect.entity.EntityAspects.resolve(living).map(value->value.aspects()).orElseGet(tc4revival.aspect.AspectAmounts::new);
            for(var aspect:aspects.aspects()) {
                int amount=aspects.amount(aspect);if(amount<=0||context.getRandom().nextFloat()>=chance)continue;
                var stack=LegacyLootTemplates.decode(result);stack.setCount(Math.max(minimum,(1+context.getRandom().nextInt(amount))/divisor));
                if(stack.getItem() instanceof StoredAspectItem stored)stored.setStoredAspects(stack,new tc4revival.aspect.AspectAmounts().add(aspect,1));
                output.accept(stack);
            }
        }

    }
    public static  class Serializer extends LootPoolSingletonContainer.Serializer<InventoryLoot> {
        public void serializeCustom(JsonObject json,InventoryLoot value,JsonSerializationContext context) { super.serializeCustom(json,value,context);json.addProperty("source",value.source);json.addProperty("chance",value.chance);json.add("result",value.result);json.addProperty("divisor",value.divisor);json.addProperty("minimum",value.minimum); }
        protected InventoryLoot deserialize(JsonObject json,JsonDeserializationContext context,int weight,int quality,LootItemCondition[] conditions,LootItemFunction[] functions) { return new InventoryLoot(GsonHelper.getAsString(json,"source"),GsonHelper.getAsFloat(json,"chance",1),GsonHelper.getAsJsonObject(json,"result",new JsonObject()),GsonHelper.getAsInt(json,"divisor",1),GsonHelper.getAsInt(json,"minimum",1),weight,quality,conditions,functions); }
    }
}
