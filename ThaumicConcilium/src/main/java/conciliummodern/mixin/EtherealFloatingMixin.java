package conciliummodern.mixin;

import conciliummodern.EtherealState;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exempts only server-authorized ethereal players from the vanilla floating check. */
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class EtherealFloatingMixin {
    @Inject(method={"noBlocksAround(Lnet/minecraft/world/entity/Entity;)Z","m_9793_(Lnet/minecraft/world/entity/Entity;)Z"},at=@At("HEAD"),cancellable=true,require=1)
    private void concilium$hover(Entity entity,CallbackInfoReturnable<Boolean> result){if(entity instanceof Player player && EtherealState.active(player))result.setReturnValue(false);}
}
