package thaumicfutures.mixin;

import com.buuz135.industrial.block.agriculturehusbandry.tile.PlantFertilizerTile;
import com.buuz135.industrial.block.tile.IndustrialWorkingTile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicfutures.compat.IndustrialForegoingCompat;
import tc4revival.content.*;

@Mixin(value = PlantFertilizerTile.class, remap = false)
public abstract class IndustrialFertilizerMixin {
    @Shadow private int powerPerOperation;
    @Inject(method = "work", at = @At("HEAD"), cancellable = true, remap = false)
    private void omniverse$growMagic(CallbackInfoReturnable<IndustrialWorkingTile.WorkAction> ci) {
        if (!thaumicfutures.FutureFeatures.Group.INDUSTRIAL.enabled()) return;
        var tile = (PlantFertilizerTile) (Object) this;
        var pos = tile.getPointedBlockPos();
        if (!(tile.getLevel() instanceof ServerLevel level) || !tile.isLoaded(pos) || !tile.hasEnergy(powerPerOperation)) return;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MagicalSaplingBlock)
                && !(state.getBlock() instanceof ManaPodBlock && state.getValue(ManaPodBlock.AGE) < 7)) return;
        ItemStack fertilizer = ItemStack.EMPTY;
        for (int i = 0; i < tile.fertilizer.getSlots(); i++) {
            if (!tile.fertilizer.getStackInSlot(i).isEmpty()) { fertilizer = tile.fertilizer.getStackInSlot(i); break; }
        }
        if (fertilizer.isEmpty() || !IndustrialForegoingCompat.allowed(level, pos)) return;
        if (state.getBlock() instanceof MagicalSaplingBlock sapling) sapling.growTree(state, level, pos, level.random);
        else if (level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) pod.grow();
        boolean changed = !level.getBlockState(pos).equals(state);
        if (changed) fertilizer.shrink(1);
        tile.increasePointer();
        ci.setReturnValue(tile.new WorkAction(.5f, changed ? powerPerOperation : 0));
    }
}
