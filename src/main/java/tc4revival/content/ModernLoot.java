package tc4revival.content;

import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.level.storage.loot.providers.number.*;
import net.minecraftforge.registries.*;

/** Context adapters only. Item selection, weights, quantities and probabilities live in loot tables. */
public  class ModernLoot {
    private static final DeferredRegister<LootItemFunctionType> FUNCTIONS=DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE,"thaumcraft");
    private static final DeferredRegister<LootItemConditionType> CONDITIONS=DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,"thaumcraft");
    private static final DeferredRegister<LootNumberProviderType> NUMBERS=DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE,"thaumcraft");
    private static final RegistryObject<LootItemFunctionType> STATE=FUNCTIONS.register("item_state",()->new LootItemFunctionType(new State.Serializer()));
    private static final RegistryObject<LootItemConditionType> NEIGHBORS=CONDITIONS.register("no_same_entity_nearby",()->new LootItemConditionType(new Neighbors.Serializer()));
    private static final RegistryObject<LootNumberProviderType> LOOTING=NUMBERS.register("looting",()->new LootNumberProviderType(new Looting.Serializer()));
    private static final RegistryObject<LootItemConditionType> ENTITY_STATE=CONDITIONS.register("entity_state",()->new LootItemConditionType(new EntityState.Serializer()));
    private ModernLoot() {}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { FUNCTIONS.register(bus);CONDITIONS.register(bus);NUMBERS.register(bus); }

    /** Linear looting level is also usable as a uniform/binomial bound in ordinary JSON providers. */
    public record Looting(float base,float multiplier) implements NumberProvider {
        public float getFloat(LootContext context) { return base+multiplier*Math.max(0,context.getLootingModifier()); }
        public LootNumberProviderType getType() { return LOOTING.get(); }
        public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<Looting> {
            public void serialize(JsonObject json,Looting value,JsonSerializationContext context) { json.addProperty("base",value.base);json.addProperty("multiplier",value.multiplier); }
            public Looting deserialize(JsonObject json,JsonDeserializationContext context) { return new Looting(GsonHelper.getAsFloat(json,"base",0),GsonHelper.getAsFloat(json,"multiplier",1)); }
        }
    }
    public record Neighbors(double radius) implements LootItemCondition {
        public boolean test(LootContext context) {
            var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);
            return entity!=null && context.getLevel().getEntities(entity,entity.getBoundingBox().inflate(radius),other->other.getType()==entity.getType()).isEmpty();
        }
        public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.THIS_ENTITY); }
        public LootItemConditionType getType() { return NEIGHBORS.get(); }
        public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<Neighbors> {
            public void serialize(JsonObject json,Neighbors value,JsonSerializationContext context) { json.addProperty("radius",value.radius); }
            public Neighbors deserialize(JsonObject json,JsonDeserializationContext context) { return new Neighbors(GsonHelper.getAsDouble(json,"radius")); }
        }
    }
    public record EntityState(String state) implements LootItemCondition {
        public boolean test(LootContext context) {
            var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);
            return switch(state) {
                case "not_summoned" -> entity instanceof FireBatEntity bat && !bat.summoned();
                case "small_slime" -> entity instanceof ThaumicSlimeEntity slime && slime.slimeSize()<3;
                default -> false;
            };
        }
        public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.THIS_ENTITY); }
        public LootItemConditionType getType() { return ENTITY_STATE.get(); }
        public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<EntityState> {
            public void serialize(JsonObject json,EntityState value,JsonSerializationContext context) { json.addProperty("state",value.state); }
            public EntityState deserialize(JsonObject json,JsonDeserializationContext context) { return new EntityState(GsonHelper.getAsString(json,"state")); }
        }
    }
    public static  class State extends LootItemConditionalFunction {
        private final JsonObject settings;
        private State(LootItemCondition[] conditions,JsonObject settings) { super(conditions);this.settings=settings.deepCopy(); }
        public LootItemFunctionType getType() { return STATE.get(); }
        protected ItemStack run(ItemStack stack,LootContext context) {
            var random=context.getRandom();
            switch (GsonHelper.getAsString(settings,"operation")) {
                case "aspect" -> {
                    String tag=GsonHelper.getAsString(settings,"aspect","");
                    if (GsonHelper.getAsBoolean(settings,"from_entity",false)) {
                        var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);
                        tag=entity instanceof WispEntity wisp?wisp.aspectTag():"";
                    }
                    if (stack.getItem() instanceof StoredAspectItem stored) {
                        var aspect=tc4revival.aspect.TCAspects.catalog().find(tag);
                        if (aspect.isEmpty()) return ItemStack.EMPTY;
                        stored.setStoredAspects(stack,new tc4revival.aspect.AspectAmounts().add(aspect.get(),GsonHelper.getAsInt(settings,"amount",1)));
                    }
                }
                case "charge" -> {
                    if(stack.getItem() instanceof VisAmuletItem amulet) for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())
                        amulet.addVis(stack,aspect,random.nextInt(GsonHelper.getAsInt(settings,"maximum",4)+1),true,true);
                }
                case "enchant" -> {
                    int base=GsonHelper.getAsInt(settings,"base",5),bound=GsonHelper.getAsInt(settings,"bound",18);
                    float multiplier=GsonHelper.getAsFloat(settings,"multiplier",0);
                    stack=LegacyRandomEnchantments.apply(stack,(int)(base+multiplier*random.nextInt(bound)),random);
                }
                default -> throw new IllegalArgumentException("Unknown loot item operation "+settings);
            }
            return stack;
        }
        public static  class Serializer extends LootItemConditionalFunction.Serializer<State> {
            public void serialize(JsonObject json,State value,JsonSerializationContext context) { super.serialize(json,value,context);json.add("settings",value.settings); }
            public State deserialize(JsonObject json,JsonDeserializationContext context,LootItemCondition[] conditions) { return new State(conditions,GsonHelper.getAsJsonObject(json,"settings")); }
        }
    }
}
