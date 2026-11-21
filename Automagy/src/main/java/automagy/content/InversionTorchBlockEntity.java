package automagy.content;

import java.util.ArrayDeque;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InversionTorchBlockEntity extends BlockEntity {
    private final ArrayDeque<Long> changes = new ArrayDeque<>();
    private long burnedUntil;
    public InversionTorchBlockEntity(BlockPos pos, BlockState state) { super(Content.INVERSION_TILE.get(), pos, state); }
    public void update() {
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        while (!changes.isEmpty() && (now - changes.peekFirst() > 60 || changes.peekFirst() > now)) changes.removeFirst();
        if (burnedUntil > now+60) burnedUntil = now+60;
        if (now < burnedUntil) { level.scheduleTick(worldPosition, getBlockState().getBlock(), (int)(burnedUntil-now)); return; }
        Direction support = getBlockState().getValue(InversionTorchBlock.FACING).getOpposite();
        int next = 15 - Math.min(15, Math.max(0, level.getSignal(worldPosition.relative(support), support)));
        if (next != getBlockState().getValue(InversionTorchBlock.POWER)) {
            changes.addLast(now);
            if (changes.size() >= 30) {
                next = 0; burnedUntil = now+60;
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.REDSTONE_TORCH_BURNOUT,
                        net.minecraft.sounds.SoundSource.BLOCKS, .5f, 2.6f);
                level.scheduleTick(worldPosition, getBlockState().getBlock(), 60);
            }
            level.setBlock(worldPosition, getBlockState().setValue(InversionTorchBlock.POWER, next), 3);
            for (Direction d : Direction.values()) level.updateNeighborsAt(worldPosition.relative(d), getBlockState().getBlock());
            setChanged();
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putLong("BurnedUntil", burnedUntil);
        tag.putLongArray("Changes", changes.stream().mapToLong(Long::longValue).toArray());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); burnedUntil = tag.getLong("BurnedUntil"); changes.clear();
        long[] history = tag.getLongArray("Changes");
        for (int i=Math.max(0, history.length-30); i<history.length; i++) changes.addLast(history[i]);
    }
    @Override public void onLoad() { super.onLoad(); if (level != null && !level.isClientSide) level.scheduleTick(worldPosition, getBlockState().getBlock(), 2); }
}
