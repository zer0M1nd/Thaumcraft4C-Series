package thaumicomniverse.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EntityMaid.class,remap=false)
public class MaidChargingTeleportMixin {
    @Inject(method="teleportToOwner",at=@At("HEAD"),cancellable=true)
    private void omniverse$retrieveWand(LivingEntity owner,CallbackInfoReturnable<Boolean> callback) {
        var maid=(EntityMaid)(Object)this;
        if(!maid.level().isClientSide && !thaumicomniverse.compat.maid.MaidCharging.reclaim(maid))callback.setReturnValue(false);
    }
}
