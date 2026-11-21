package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.content.WorkbenchChargerBlockEntity;

/** Native processing machines sit two blocks above a belt; a relay sits directly on it. */
@Mixin(value=BeltInventory.class,remap=false)
public abstract class CreateBeltChargerMixin {
    @Shadow @Final private BeltBlockEntity belt;
    @Inject(method="getBeltProcessingAtSegment",at=@At("HEAD"),cancellable=true)
    private void omniverse$charger(int segment,CallbackInfoReturnable<BeltProcessingBehaviour> cir) {
        if(!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()||belt.getLevel()==null)return;
        var pos=BeltHelper.getPositionForOffset(belt,segment).above();
        if(belt.getLevel().getBlockEntity(pos) instanceof WorkbenchChargerBlockEntity charger&&charger.extendedSupport())
            cir.setReturnValue(BlockEntityBehaviour.get(charger,BeltProcessingBehaviour.TYPE));
        if(belt.getLevel().getBlockEntity(pos) instanceof tc4revival.content.ItemMirrorBlockEntity mirror)
            cir.setReturnValue(BlockEntityBehaviour.get(mirror,BeltProcessingBehaviour.TYPE));
    }
    @Redirect(method="handleBeltProcessingAndCheckIfRemoved",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour;isBlocked(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean omniverse$chargingSpace(net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos) {
        if(thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()
                &&level.getBlockEntity(pos.above()) instanceof WorkbenchChargerBlockEntity charger&&charger.extendedSupport())return false;
        if(thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()
                &&level.getBlockEntity(pos.above()) instanceof tc4revival.content.ItemMirrorBlockEntity)return false;
        return BeltProcessingBehaviour.isBlocked(level,pos);
    }
}
