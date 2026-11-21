package tc4revival.content;

import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Produces the portable legacy payload, not a copy of internal machine NBT or queued inventory. */
public  class PortableBlockLoot extends LootItemConditionalFunction {
    private static final DeferredRegister<LootItemFunctionType> TYPES=DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE,"thaumcraft");
    private static final RegistryObject<LootItemFunctionType> TYPE=TYPES.register("portable_block",()->new LootItemFunctionType(new Serializer()));
    private PortableBlockLoot(LootItemCondition[] conditions) { super(conditions); }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static LootItemConditionalFunction.Builder<?> portable() { return simpleBuilder(PortableBlockLoot::new); }
    @Override public LootItemFunctionType getType() { return TYPE.get(); }
    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.BLOCK_ENTITY); }
    @Override protected ItemStack run(ItemStack stack,LootContext context) {
        var entity=context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof EssentiaJarBlockEntity jar) return jar.asItem();
        if (entity instanceof NodeJarBlockEntity jar) return jar.drop?jar.asItem():ItemStack.EMPTY;
        if (entity instanceof ItemMirrorBlockEntity mirror && mirror.linked() && mirror.destination()!=null){
            var result=ItemMirrorItem.linkedStack(mirror.destination(),context.getLevel().dimension().location().toString());
            if(mirror.targetIdentity()!=null)result.getOrCreateTag().putUUID("MirrorTargetIdentity",mirror.targetIdentity());return result;
        }
        if (entity instanceof EssentiaMirrorBlockEntity mirror && mirror.linked() && mirror.mirrorDestination()!=null)
            return EssentiaMirrorItem.linkedStack(mirror.mirrorDestination(),context.getLevel().dimension().location().toString());
        // Mirror fallback remains a plain mirror; jars and banners require their block entity.
        return stack.is(TCItems.ITEM_MIRROR.get()) || stack.is(TCItems.ESSENTIA_MIRROR.get()) ? stack : ItemStack.EMPTY;
    }
    public static  class Serializer extends LootItemConditionalFunction.Serializer<PortableBlockLoot> {
        @Override public PortableBlockLoot deserialize(JsonObject json,JsonDeserializationContext context,LootItemCondition[] conditions) {
            return new PortableBlockLoot(conditions);
        }
    }
}
