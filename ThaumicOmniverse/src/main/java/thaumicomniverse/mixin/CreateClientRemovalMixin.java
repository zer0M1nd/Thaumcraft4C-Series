package thaumicomniverse.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ClientLevel.class,remap=false)
public abstract class CreateClientRemovalMixin {
    @Inject(method={"removeEntity","m_171642_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$defer(int id,Entity.RemovalReason reason,CallbackInfo ci){
        if(thaumicomniverse.compat.create.ContraptionRenderHandoff.defer((ClientLevel)(Object)this,id,reason))ci.cancel();
    }
}
