package arcane_engineering.mixin;

import arcane_engineering.BootsNetwork;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Official/SRG aliases follow the workspace's Forge 1.20.1 mixin convention. */
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class ImpulseFlightMixin {
    @Shadow(aliases={"f_9736_"},remap=false) private boolean clientIsFloating;
    @Shadow(aliases={"f_9737_"},remap=false) private int aboveGroundTickCount;

    @Inject(method={"tick()V","m_9933_()V"},at=@At("HEAD"),remap=false)
    private void arcaneEngineering$authorizedThrust(CallbackInfo callback) {
        var connection=(ServerGamePacketListenerImpl)(Object)this;
        if(BootsNetwork.hasFlightPermit(connection.player)) {
            clientIsFloating=false;
            aboveGroundTickCount=0;
        }
    }
}
