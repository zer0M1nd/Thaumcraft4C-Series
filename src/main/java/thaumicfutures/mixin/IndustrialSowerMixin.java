package thaumicfutures.mixin;

import com.buuz135.industrial.block.agriculturehusbandry.tile.PlantSowerTile;
import com.buuz135.industrial.block.tile.IndustrialWorkingTile;
import com.hrznstudio.titanium.component.bundle.LockableInventoryBundle;
import com.hrznstudio.titanium.filter.ItemStackFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.content.*;

@Mixin(value = PlantSowerTile.class, remap = false)
public abstract class IndustrialSowerMixin {
    @Shadow private LockableInventoryBundle<PlantSowerTile> input;
    @Shadow private ItemStackFilter filter;
    @Shadow private int powerPerOperation;
    @Shadow protected abstract int getFilteredSlot(BlockPos pos);
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void omniverse$acceptBeans(BlockPos pos, BlockState state, CallbackInfo ci) {
        input.getInventory().setInputFilter((stack, slot) -> thaumicfutures.FutureFeatures.Group.INDUSTRIAL.enabled() && stack.is(TCItems.MANA_BEAN.get())
                || stack.getItem() instanceof BlockItem block && block.getBlock() instanceof IPlantable);
    }
    @Inject(method = "work", at = @At("HEAD"), cancellable = true, remap = false)
    private void omniverse$plantBean(CallbackInfoReturnable<IndustrialWorkingTile.WorkAction> ci) {
        if (!thaumicfutures.FutureFeatures.Group.INDUSTRIAL.enabled()) return;
        var tile = (PlantSowerTile) (Object) this;
        var pos = tile.getPointedBlockPos();
        if (!(tile.getLevel() instanceof ServerLevel level) || !tile.isLoaded(pos)
                || !level.isEmptyBlock(pos) || !tile.hasEnergy(powerPerOperation)) return;
        ItemStack desired = filter.getFilterSlots()[getFilteredSlot(pos)].getFilter();
        ItemStack seed = ItemStack.EMPTY;
        for (int slot = 0; slot < input.getInventory().getSlots(); slot++) {
            ItemStack found = input.getInventory().getStackInSlot(slot);
            if (!found.isEmpty() && (desired.isEmpty() || ItemStack.isSameItem(found, desired))) { seed = found; break; }
        }
        if (!seed.is(TCItems.MANA_BEAN.get())) return;
        var state = TCArcane.MANA_POD.get().defaultBlockState();
        if (!state.canSurvive(level, pos)) return;
        var player = com.buuz135.industrial.IndustrialForegoing.getFakePlayer(level);
        if (!level.mayInteract(player, pos)) return;
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, 3)) return;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.DOWN)) { snapshot.restore(true, false); return; }
        var aspects = TCItems.MANA_BEAN.get().storedAspects(seed);
        if (!aspects.isEmpty() && level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod)
            pod.setAspect(aspects.aspects().get(0));
        seed.shrink(1);
        tile.increasePointer();
        ci.setReturnValue(tile.new WorkAction(.2f, powerPerOperation));
    }
}
