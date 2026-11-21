package thaumicomniverse.mixin;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.AuraFilling;

/** A partially charged wand stays on the belt/depot and resumes after the tank is replenished. */
@Mixin(value=SpoutBlockEntity.class,remap=false)
public abstract class CeiAuraSpoutMixin {
    @Shadow public int processingTicks;
    @Shadow public boolean sendSplash;
    @Shadow SmartFluidTankBehaviour tank;
    @Inject(method="whenItemHeld",at=@At("HEAD"),cancellable=true)
    private void omniverse$charge(TransportedItemStack transported,TransportedItemStackHandlerBehaviour handler,CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir){
        if(!AuraFilling.chargeable(transported.stack) || !thaumicomniverse.RevivalFeatures.Group.CREATE.enabled())return;
        var fluid=tank.getPrimaryHandler().getFluid();if(!fluid.isEmpty() && !AuraFilling.liquid(fluid))return;
        cir.setReturnValue(BeltProcessingBehaviour.ProcessingResult.HOLD);
        if(fluid.isEmpty() || processingTicks!=-1 && processingTicks!=5)return;
        var source=(SpoutBlockEntity)(Object)this;
        if(processingTicks==-1){processingTicks=20;source.notifyUpdate();return;}
        var copy=transported.stack.copy();var updated=AuraFilling.process(copy,fluid,AuraFilling.amount(copy,fluid));
        if(updated.isEmpty())return;
        var result=transported.copy();result.stack=updated;
        var outcome=AuraFilling.missing(updated)>0
                ?TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(),result)
                :TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(result),null);
        handler.handleProcessingOnItem(transported,outcome);
        tank.getPrimaryHandler().setFluid(fluid);sendSplash=true;source.notifyUpdate();
    }
}
