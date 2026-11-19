package thaumicomniverse;

import com.google.gson.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Evaluate at loot generation, after SERVER settings exist, rather than during the initial datapack load. */
public record FeatureLootCondition(RevivalFeatures.Group group) implements LootItemCondition {
    private static final DeferredRegister<LootItemConditionType> TYPES=DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,"thaumicomniverse");
    private static final RegistryObject<LootItemConditionType> TYPE=TYPES.register("feature",()->new LootItemConditionType(new Codec()));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public LootItemConditionType getType() { return TYPE.get(); }
    @Override public boolean test(LootContext context) { return group.enabled(); }
    public static JsonObject json(RevivalFeatures.Group group) {
        var json=new JsonObject();json.addProperty("condition","thaumicomniverse:feature");json.addProperty("group",group.name());return json;
    }
    private static class Codec implements net.minecraft.world.level.storage.loot.Serializer<FeatureLootCondition> {
        @Override public void serialize(JsonObject json,FeatureLootCondition value,JsonSerializationContext context) { json.addProperty("group",value.group.name()); }
        @Override public FeatureLootCondition deserialize(JsonObject json,JsonDeserializationContext context) { return new FeatureLootCondition(RevivalFeatures.Group.valueOf(json.get("group").getAsString())); }
    }
}
