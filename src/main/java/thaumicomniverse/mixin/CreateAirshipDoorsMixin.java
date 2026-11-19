package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.airship.*;

@Mixin(value=SlidingDoorMovementBehaviour.class,remap=false)
public abstract class CreateAirshipDoorsMixin {
    @Inject(method="shouldOpen",at=@At("HEAD"),cancellable=true)
    private void omniverse$airshipDock(MovementContext context,CallbackInfoReturnable<Boolean> result){
        if(context.contraption.entity instanceof AirshipEntity ship)result.setReturnValue(!context.disabled&&ship.doorsOpen(context));
    }
    @Inject(method="shouldUpdate",at=@At("HEAD"),cancellable=true)
    private void omniverse$initialDock(MovementContext context,boolean open,CallbackInfoReturnable<Boolean> result){
        // A newly assembled airship is already docked. Create suppresses opening
        // before first movement, which would leave its doors closed indefinitely.
        if(context.contraption.entity instanceof AirshipEntity&&context.firstMovement){
            boolean changed=!context.data.contains("Open")||context.data.getBoolean("Open")!=open;
            context.data.putBoolean("Open",open);result.setReturnValue(changed);
        }
    }
}
