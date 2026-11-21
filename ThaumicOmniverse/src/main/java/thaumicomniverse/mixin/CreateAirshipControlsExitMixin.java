package thaumicomniverse.mixin;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket;
import com.simibubi.create.foundation.utility.ControlsUtil;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.airship.AirshipClient;
import thaumicomniverse.compat.create.airship.AirshipEntity;

@Mixin(value=ControlsHandler.class,remap=false)
public abstract class CreateAirshipControlsExitMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private static void omniverse$releaseOnShift(CallbackInfo ci){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null||!(ControlsHandler.getContraption() instanceof AirshipEntity ship)
                ||!AirshipClient.shiftStopsControlling(ship)||!ControlsUtil.isActuallyPressed(mc.options.keyShift))return;
        var pos=ControlsHandler.getControlsPos();
        // Match Create's Esc path, including the explicit server stop packet.
        ControlsHandler.stopControlling();
        AllPackets.getChannel().sendToServer(new ControlsInputPacket(java.util.List.of(),false,ship.getId(),pos,true));
        mc.options.keyShift.setDown(false);
        ci.cancel();
    }
}
