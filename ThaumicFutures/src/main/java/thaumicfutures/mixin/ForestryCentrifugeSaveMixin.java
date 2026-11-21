package thaumicfutures.mixin;

import forestry.factory.tiles.TileCentrifuge;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forestry's internal inventory changes do not notify its owning block entity. */
@Mixin(value=TileCentrifuge.class,remap=false)
public abstract class ForestryCentrifugeSaveMixin {
    @Inject(method="workCycle",at=@At("RETURN"),remap=false)
    private void futures$saveCompletedWork(CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValueZ())((BlockEntity)(Object)this).setChanged();
    }
}
