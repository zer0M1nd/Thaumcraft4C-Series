package thmodern.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thmodern.vat.VatContainment;

/** Stop local prediction and server physics together, instead of issuing corrective teleports every tick. */
@Mixin(value=Player.class,remap=false)
public abstract class VatPlayerTravelMixin {
    @Inject(method={"travel(Lnet/minecraft/world/phys/Vec3;)V","m_7023_"},at=@At("HEAD"),cancellable=true)
    private void th$vatTravel(Vec3 input,CallbackInfo callback) {
        if(VatContainment.restrain((Player)(Object)this))callback.cancel();
    }
}
