package thaumicfutures.compat.bees;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;

public final class VisAuraProvider extends BlockEntity {
    private static final int[] INTERVALS = {11, 5, 3};
    private static final Aspect[] ASPECTS = {Aspect.WATER, Aspect.ENTROPY, Aspect.AIR};
    private final int[] charges = new int[3], partial = new int[3];
    public VisAuraProvider(BlockPos pos, BlockState state) { super(BeeAuraContent.PROVIDER_TILE.get(), pos, state); }
    public void tick(Level world) {
        if (world.isClientSide || !thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return;
        for (int i = 0; i < charges.length; i++) {
            if (charges[i] >= 10 || world.getGameTime() % INTERVALS[i] != 0) continue;
            int drawn = VisNetHandler.drainVis(world, worldPosition, ASPECTS[i], 1);
            if (drawn <= 0) continue;
            partial[i] += drawn;
            if (partial[i] >= 8) { partial[i] -= 8; charges[i]++; }
            setChanged();
        }
    }
    public boolean takeCharge(int channel) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled() || level == null || level.isClientSide || channel < 0 || channel >= charges.length || charges[channel] == 0) return false;
        charges[channel]--;
        setChanged();
        return true;
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        int[] stored = tag.getIntArray("Charges"), remainder = tag.getIntArray("PartialVis");
        for (int i = 0; i < 3; i++) {
            charges[i] = i < stored.length ? Math.max(0, Math.min(10, stored[i])) : 0;
            partial[i] = i < remainder.length ? Math.max(0, Math.min(7, remainder[i])) : 0;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Charges", charges);
        tag.putIntArray("PartialVis", partial);
    }
}
