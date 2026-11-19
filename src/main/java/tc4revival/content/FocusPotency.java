package tc4revival.content;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/** Fractional power stays continuous until an effect requires an indivisible count. */
public class FocusPotency {
    public static float bonus(LivingEntity entity) {
        if (entity == null || !entity.getAttributes().hasAttribute(TCAttributes.POTENCY.get())) return 0;
        return (float) entity.getAttributeValue(TCAttributes.POTENCY.get());
    }
    public static int discrete(float amount, RandomSource random) {
        if (!Float.isFinite(amount) || amount <= 0) return 0;
        int whole = (int) amount;
        float fraction = amount - whole;
        return whole + (fraction > 0 && random.nextFloat() < fraction ? 1 : 0);
    }
}
