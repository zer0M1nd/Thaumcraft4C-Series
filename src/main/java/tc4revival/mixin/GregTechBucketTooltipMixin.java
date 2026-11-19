package tc4revival.mixin;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tc4revival.compat.BucketTooltipFluids;

/** GTCEu 7.5.3 assumes all mod buckets expose a non-null fluid during creative search indexing. */
@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.client.TooltipsHandler", remap = false)
public abstract class GregTechBucketTooltipMixin {
    @Redirect(method = "appendTooltips", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BucketItem;getFluid()Lnet/minecraft/world/level/material/Fluid;"))
    private static Fluid tc4$bucketTooltipFluid(BucketItem bucket) {
        return BucketTooltipFluids.forTooltip(bucket.getFluid());
    }
}
