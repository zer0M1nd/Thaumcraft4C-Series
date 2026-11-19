package tc4revival.content;

import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Suppression belongs to the loot entry: an EMPTY result still occupies a raw loot list slot. */
public  class PortableBlockAvailable implements LootItemCondition {
    private static final DeferredRegister<LootItemConditionType> TYPES=DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,"thaumcraft");
    private static final RegistryObject<LootItemConditionType> TYPE=TYPES.register("portable_block_available",()->new LootItemConditionType(new Serializer()));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static LootItemCondition.Builder available() { return PortableBlockAvailable::new; }
    @Override public LootItemConditionType getType() { return TYPE.get(); }
    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.BLOCK_STATE,LootContextParams.BLOCK_ENTITY); }
    @Override public boolean test(LootContext context) {
        var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        var entity=context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if(state==null)return false;
        if(state.getBlock() instanceof EssentiaJarBlock)return entity instanceof EssentiaJarBlockEntity;
        if(state.getBlock() instanceof NodeJarBlock)return entity instanceof NodeJarBlockEntity jar&&jar.drop;
        return state.getBlock() instanceof ItemMirrorBlock||state.getBlock() instanceof EssentiaMirrorBlock;
    }
    public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<PortableBlockAvailable> {
        @Override public void serialize(JsonObject json,PortableBlockAvailable value,JsonSerializationContext context) {}
        @Override public PortableBlockAvailable deserialize(JsonObject json,JsonDeserializationContext context) { return new PortableBlockAvailable(); }
    }
}
