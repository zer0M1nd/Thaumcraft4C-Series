package thaumicfutures.foci;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicfutures.LegacyContent;

/** TC5 spell foci, independent of wand construction materials. */
public final class FutureFoci {
    public static final RegistryObject<ShardFocusItem> SHARD = LegacyContent.ITEMS.register("focus_shard", ShardFocusItem::new);
    public static final RegistryObject<GrappleFocusItem> GRAPPLE = LegacyContent.ITEMS.register("focus_grapple", GrappleFocusItem::new);
    public static final RegistryObject<BuilderFocusItem> BUILDER = LegacyContent.ITEMS.register("focus_builder", BuilderFocusItem::new);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "thaumicfutures");
    public static final RegistryObject<EntityType<HomingShardEntity>> SHARD_ENTITY = ENTITIES.register("homing_shard", () ->
            EntityType.Builder.<HomingShardEntity>of(HomingShardEntity::new, MobCategory.MISC).sized(.25f, .25f)
                    .clientTrackingRange(8).updateInterval(1).build("thaumicfutures:homing_shard"));
    public static final RegistryObject<EntityType<GrappleEntity>> GRAPPLE_ENTITY = ENTITIES.register("grapple", () ->
            EntityType.Builder.<GrappleEntity>of(GrappleEntity::new, MobCategory.MISC).sized(.1f, .1f)
                    .clientTrackingRange(10).updateInterval(1).build("thaumicfutures:grapple"));
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "thaumicfutures");
    public static final RegistryObject<SoundEvent> SCAN = sound("focus_scan"), ZAP = sound("focus_zap"), ICE = sound("focus_ice");
    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumicfutures:" + name)));
    }
    public static void register(IEventBus bus) { ENTITIES.register(bus); SOUNDS.register(bus); }
    private FutureFoci() {}
}
