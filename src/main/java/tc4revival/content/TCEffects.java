package tc4revival.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public  class TCEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "thaumcraft");
    public static final RegistryObject<MobEffect> VIS_EXHAUST = EFFECTS.register("vis_exhaust", () -> new Exhaustion(false));
    public static final RegistryObject<MobEffect> INFECTIOUS_VIS_EXHAUST = EFFECTS.register("infectious_vis_exhaust", () -> new Exhaustion(true));
    private TCEffects() {}
    public static final RegistryObject<MobEffect> FLUX_TAINT = EFFECTS.register("flux_taint", FluxTaintEffect::new);
    public static final RegistryObject<MobEffect> UNNATURAL_HUNGER = EFFECTS.register("unnatural_hunger",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.HUNGER));
    public static final RegistryObject<MobEffect> WARP_WARD = EFFECTS.register("warp_ward",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.WARD));
    public static final RegistryObject<MobEffect> DEATH_GAZE = EFFECTS.register("death_gaze",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.GAZE));
    public static final RegistryObject<MobEffect> BLURRED_VISION = EFFECTS.register("blurred_vision",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.BLURRED));
    public static final RegistryObject<MobEffect> SUN_SCORNED = EFFECTS.register("sun_scorned",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.SUN));
    public static final RegistryObject<MobEffect> THAUMARHIA = EFFECTS.register("thaumarhia",()->new LegacyWarpEffect(LegacyWarpEffect.Kind.THAUMARHIA));
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
    private static  class Exhaustion extends MobEffect {
        private final boolean infectious;
        Exhaustion(boolean infectious) { super(MobEffectCategory.HARMFUL, infectious ? 0x665577 : 0x664477); this.infectious = infectious; }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return infectious && duration % 40 == 0; }
        @Override public void applyEffectTick(LivingEntity source, int amplifier) {
            if (!infectious || source.level().isClientSide) return;
            for (var target : source.level().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(4))) {
                if (target.hasEffect(INFECTIOUS_VIS_EXHAUST.get())) continue;
                target.addEffect(new MobEffectInstance(amplifier > 0 ? INFECTIOUS_VIS_EXHAUST.get() : VIS_EXHAUST.get(), 6000, Math.max(0, amplifier - 1), false, true));
            }
        }
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
            consumer.accept(new tc4revival.client.LegacyEffectIcon(infectious ? 6 : 5, 1));
        }
    }
}
