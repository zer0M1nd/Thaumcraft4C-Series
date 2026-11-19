package tc4revival.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/** Standalone TC4 extends the vanilla 32-slot array to 40, filling slots 31 through 39. */
public  class ManaBeanEffects {
    public static final int SLOTS=40;
    private ManaBeanEffects() {}
    public static MobEffect at(int slot) {
        if (slot>=1 && slot<=19) return net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                net.minecraft.resources.ResourceLocation.parse(tc4revival.aspect.item.PotionAspectRules.modernEffect(slot)));
        return switch(slot) {
            case 20 -> MobEffects.WITHER; case 21 -> MobEffects.HEALTH_BOOST;
            case 22 -> MobEffects.ABSORPTION; case 23 -> MobEffects.SATURATION;
            case 31 -> TCEffects.FLUX_TAINT.get(); case 32 -> TCEffects.VIS_EXHAUST.get();
            case 33 -> TCEffects.INFECTIOUS_VIS_EXHAUST.get(); case 34 -> TCEffects.UNNATURAL_HUNGER.get();
            case 35 -> TCEffects.WARP_WARD.get(); case 36 -> TCEffects.DEATH_GAZE.get();
            case 37 -> TCEffects.BLURRED_VISION.get(); case 38 -> TCEffects.SUN_SCORNED.get();
            case 39 -> TCEffects.THAUMARHIA.get(); default -> null;
        };
    }
}
