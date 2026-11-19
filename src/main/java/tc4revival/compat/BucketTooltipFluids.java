package tc4revival.compat;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** A missing bucket fluid has no fluid tooltip; do not change bucket contents or capabilities. */
public class BucketTooltipFluids {
    public static Fluid forTooltip(Fluid fluid) {
        return fluid == null ? Fluids.EMPTY : fluid;
    }
}
