package heartwork.compat.blade;

import com.google.gson.*;
import heartwork.Heartwork;
import java.io.*;
import java.nio.charset.StandardCharsets;
import mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Loaded only after the optional modern SlashBlade host has been detected. */
public final class BladeCompat {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,Heartwork.ID);
    public static final RegistryObject<RecipeSerializer<HeartworkBladeRecipe>> CRAFTING=RECIPES.register("blade_crafting",HeartworkBladeRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<WrapBladeRecipe>> WRAPPING=RECIPES.register("wrap_blade",WrapBladeRecipe.Serializer::new);
    public static final DeferredRegister<SlashArts> ARTS=DeferredRegister.create(SlashArts.REGISTRY_KEY,Heartwork.ID);
    public static final DeferredRegister<ComboState> COMBOS=DeferredRegister.create(ComboState.REGISTRY_KEY,Heartwork.ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,Heartwork.ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<HeartworkCutEntity>> CUT=ENTITIES.register("sanhua",() ->
            net.minecraft.world.entity.EntityType.Builder.<HeartworkCutEntity>of(HeartworkCutEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(4,4).clientTrackingRange(10).updateInterval(1).build("heartwork:sanhua"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<HeartworkSummonedSword>> SWORDS=ENTITIES.register("blistering_sword",() ->
            net.minecraft.world.entity.EntityType.Builder.<HeartworkSummonedSword>of(HeartworkSummonedSword::new,net.minecraft.world.entity.MobCategory.MISC).sized(0.5F,0.5F).clientTrackingRange(10).updateInterval(1).build("heartwork:blistering_sword"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<HeartworkDriveEntity>> DRIVE=ENTITIES.register("drive",() ->
            net.minecraft.world.entity.EntityType.Builder.<HeartworkDriveEntity>of(HeartworkDriveEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(1,2).clientTrackingRange(10).updateInterval(1).build("heartwork:drive"));
    public static final JsonObject METADATA=load();
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,Heartwork.ID);
    private static JsonObject load() {
        try(var reader=new InputStreamReader(java.util.Objects.requireNonNull(BladeCompat.class.getResourceAsStream("/data/heartwork/blades.json")),StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch(IOException ex) { throw new UncheckedIOException(ex); }
    }
    public static void register(IEventBus bus) {
        for(String kind:new String[]{"thw","tls","void","xf"})ITEMS.register(kind+"_blade",() -> new HeartworkBladeItem(kind));
        for(String name:new String[]{"unnamed_blade","model_blade","model_blade_1"})ITEMS.register(name,() -> new BasicBladeItem(!name.equals("unnamed_blade")));
        ITEMS.register("rf_blade",FluxBladeItem::new);
        ITEMS.register("wrapped_blade",WrappedBladeItem::new);
        if(net.minecraftforge.fml.ModList.get().isLoaded("ic2"))ITEMS.register("eu_blade",ElectricBladeItem::new);
        HeartworkArts.register();
        RECIPES.register(bus);COMBOS.register(bus);ARTS.register(bus);ENTITIES.register(bus);ITEMS.register(bus);
        BladeNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(BladeCompat::beforeCreate);
        MinecraftForge.EVENT_BUS.addListener(BladeCompat::onCreate);
        MinecraftForge.EVENT_BUS.addListener(BladeCompat::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event) -> {
            var entity=event.getEntity();
            if(entity.getPersistentData().getLong("HeartworkTeleportBlockedUntil")>=entity.level().getGameTime())event.setCanceled(true);
        });
        if(net.minecraftforge.fml.ModList.get().isLoaded("botania"))MinecraftForge.EVENT_BUS.addListener(BotaniaBladeCompat::tickStand);
    }
    private static void beforeCreate(SlashBladeRegistryEvent.Pre event) {
        var entry=METADATA.get(event.getSlashBladeDefinition().getName().toString());
        if(entry!=null&&entry.getAsJsonObject().has("required_mod")&&!net.minecraftforge.fml.ModList.get().isLoaded(entry.getAsJsonObject().get("required_mod").getAsString()))event.setCanceled(true);
    }
    private static void onCreate(SlashBladeRegistryEvent.Post event) {
        String id=event.getSlashBladeDefinition().getName().toString();
        if(!METADATA.has(id)) return;
        var meta=METADATA.getAsJsonObject(id);
        event.getBlade().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if(meta.has("attack_amplifier"))state.setAttackAmplifier(meta.get("attack_amplifier").getAsFloat());
            event.getBlade().getOrCreateTag().put("bladeState",state.serializeNBT());
        });
        event.getBlade().getOrCreateTag().putString("HeartworkBlade",id);
    }
    public static ResourceLocation id(String name) { return new ResourceLocation(Heartwork.ID,name); }
    private static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        var user=event.getEntity();if(user.level().isClientSide)return;
        int ticks=user.getPersistentData().getInt("HeartworkSpearTicks");
        if(ticks>0) {
            user.getPersistentData().putInt("HeartworkSpearTicks",ticks-1);
            HeartworkArts.spearTick(user,8-ticks);
        }
    }
}
