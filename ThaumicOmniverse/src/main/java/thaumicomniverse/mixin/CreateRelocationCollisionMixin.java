package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.WandContraptionEntity;

/** Ballistic sweeps own impact and knockback; native prediction would push victims ahead of them. */
@Mixin(value=ContraptionCollider.class,remap=false)
public abstract class CreateRelocationCollisionMixin {
    @Inject(method="collideEntities",at=@At("HEAD"),cancellable=true)
    private static void omniverse$projectileCollision(AbstractContraptionEntity entity,CallbackInfo ci){
        if(entity.getRootVehicle() instanceof WandContraptionEntity projectile&&projectile.isProjectile())ci.cancel();
    }
}
