package thaumicfutures.mixin;

import com.reborn.modularmachinery.block.MachineControllerBlockEntity;
import com.reborn.modularmachinery.machine.HatchCollection;
import com.reborn.modularmachinery.machine.MachineDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicfutures.compat.machinery.MachineAspectPorts;

@Mixin(value = MachineControllerBlockEntity.class, remap = false)
public abstract class MachineryPortsMixin {
    @Inject(method = "collectHatches", at = @At("RETURN"))
    private void omniverse$ports(Level level, BlockPos pos, MachineDefinition machine, CallbackInfoReturnable<HatchCollection> callback) {
        MachineAspectPorts.collect(callback.getReturnValue(), level, pos, machine);
    }
}
