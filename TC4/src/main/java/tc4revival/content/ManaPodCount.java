package tc4revival.content;

import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Rolls beans separately and preserves the old strict > .33 mature-pod threshold. */
public  class ManaPodCount implements NumberProvider {
    private static final DeferredRegister<LootNumberProviderType> TYPES=DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE,"thaumcraft");
    private static final RegistryObject<LootNumberProviderType> TYPE=TYPES.register("mana_pod_count",()->new LootNumberProviderType(new Serializer()));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public int getInt(LootContext context) {
        var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if(state==null || !state.hasProperty(ManaPodBlock.AGE)) return 0;
        int age=state.getValue(ManaPodBlock.AGE);
        return ManaPodGrowth.harvestCount(age,age==7?context.getLevel().random.nextFloat():0);
    }
    @Override public float getFloat(LootContext context) { return getInt(context); }
    @Override public LootNumberProviderType getType() { return TYPE.get(); }
    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.BLOCK_STATE); }
    public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ManaPodCount> {
        @Override public void serialize(JsonObject json,ManaPodCount value,JsonSerializationContext context) {}
        @Override public ManaPodCount deserialize(JsonObject json,JsonDeserializationContext context) { return new ManaPodCount(); }
    }
}
