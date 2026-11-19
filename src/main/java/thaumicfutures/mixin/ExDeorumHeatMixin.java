package thaumicfutures.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thedarkcolour.exdeorum.blockentity.LavaCrucibleBlockEntity;
import thedarkcolour.exdeorum.recipe.crucible.CrucibleHeatRecipe;
import thaumicfutures.compat.exdeorum.ExDeorumCompat;

/** The old 0.15 vs torch 0.1 heat maps to alternating 1/2 mB in native ten-tick steps. */
@Mixin(value=LavaCrucibleBlockEntity.class,remap=false)
public abstract class ExDeorumHeatMixin {
    @Inject(method="getMeltingRate",at=@At("RETURN"),cancellable=true)
    private void nitorHeat(CallbackInfoReturnable<Integer> result) {
        var tile=(BlockEntity)(Object)this;
        var level=tile.getLevel();
        if(level==null||!ExDeorumCompat.enabled()||result.getReturnValueI()!=2) return;
        var recipe=level.getRecipeManager().byKey(ResourceLocation.parse("thaumicfutures:exdeorum/heat_nitor")).orElse(null);
        if(recipe instanceof CrucibleHeatRecipe heat && heat.heatValue()==2
                && heat.blockPredicate().test(level.getBlockState(tile.getBlockPos().below())))
            result.setReturnValue(1+(int)((level.getGameTime()/10)&1));
    }
}
