package thaumicomniverse.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFollowOwnerTask;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retrieval must finish before ordinary following can replace its walk target. */
@Mixin(value=MaidFollowOwnerTask.class,remap=false)
public class MaidChargingFollowMixin {
    @Inject(method="checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Z",at=@At("HEAD"),cancellable=true)
    private void omniverse$finishCharging(ServerLevel level,EntityMaid maid,CallbackInfoReturnable<Boolean> callback) {
        if(thaumicomniverse.compat.maid.MaidCharging.pending(maid))callback.setReturnValue(false);
    }
}
