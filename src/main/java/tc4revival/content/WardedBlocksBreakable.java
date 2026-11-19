package tc4revival.content;

import com.google.gson.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public  class WardedBlocksBreakable implements LootItemCondition {
    private static final DeferredRegister<LootItemConditionType> TYPES=DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,"thaumcraft");
    private static final RegistryObject<LootItemConditionType> TYPE=TYPES.register("warded_blocks_breakable",()->new LootItemConditionType(new Serializer()));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static LootItemCondition.Builder condition() { return WardedBlocksBreakable::new; }
    @Override public LootItemConditionType getType() { return TYPE.get(); }
    @Override public boolean test(LootContext context) { return !tc4revival.config.TCConfig.SERVER.wardedStone.get(); }
    public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<WardedBlocksBreakable> {
        @Override public void serialize(JsonObject json,WardedBlocksBreakable value,JsonSerializationContext context) {}
        @Override public WardedBlocksBreakable deserialize(JsonObject json,JsonDeserializationContext context) { return new WardedBlocksBreakable(); }
    }
}
