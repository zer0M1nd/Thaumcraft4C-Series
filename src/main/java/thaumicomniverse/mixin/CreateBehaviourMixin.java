package thaumicomniverse.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockEntityBehaviour.class, remap = false)
public abstract class CreateBehaviourMixin {
    @Inject(method = "get(Lnet/minecraft/world/level/block/entity/BlockEntity;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void omniverse$pedestal(BlockEntity entity, BehaviourType<?> type, CallbackInfoReturnable<BlockEntityBehaviour> cir) {
        if (!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()) return;
        thaumicomniverse.compat.create.CreateDevices.bridge(entity).ifPresent(bridge->cir.setReturnValue(bridge.getBehaviour(type)));
    }
}
