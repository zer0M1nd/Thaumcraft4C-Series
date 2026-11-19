package thaumicomniverse.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.maid.MaidCharging;

/** Servant bells and trumpets use Entity.teleportTo instead of teleportToOwner. */
@Mixin(value=Entity.class,remap=false)
public class MaidDirectTeleportMixin {
    @Inject(method={"teleportTo(DDD)V","m_6021_(DDD)V"},at=@At("HEAD"),cancellable=true,require=1)
    private void omniverse$retrieveBeforeTeleport(double x,double y,double z,CallbackInfo callback) {
        if((Object)this instanceof EntityMaid maid && !maid.level().isClientSide
                && MaidCharging.pending(maid) && !MaidCharging.reclaim(maid))callback.cancel();
    }
    // Commands and some integrations use this overload, which copies the entity to
    // the destination world without posting EntityTravelToDimensionEvent.
    @Inject(method={"teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
            "m_264318_(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z"},at=@At("HEAD"),cancellable=true,require=1)
    private void omniverse$retrieveBeforeWorldTeleport(net.minecraft.server.level.ServerLevel destination,
            double x,double y,double z,java.util.Set<net.minecraft.world.entity.RelativeMovement> relative,float yaw,float pitch,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback) {
        if((Object)this instanceof EntityMaid maid && !maid.level().isClientSide
                && MaidCharging.pending(maid) && !MaidCharging.reclaim(maid))callback.setReturnValue(false);
    }
}
