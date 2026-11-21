package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import tc4revival.network.FocusEffectPacket;

public  class NodeConverterBlockEntity extends BlockEntity {
    private int count = -1, status;
    private boolean converting;
    public NodeConverterBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.NODE_CONVERTER_ENTITY.get(), pos, state); }
    public int count() { return count; }
    public int status() { return status; }
    private boolean surroundingsLoaded() { return level != null && level.hasChunkAt(worldPosition.below(2)) && level.hasChunkAt(worldPosition.below()); }
    public boolean hasStabilizer() {
        return surroundingsLoaded() && !level.hasNeighborSignal(worldPosition.below(2)) && level.getBlockEntity(worldPosition.below(2)) instanceof NodeStabilizerBlockEntity;
    }
    public void checkStatus() {
        if (level == null || level.isClientSide || converting || !surroundingsLoaded()) return;
        if (count < 0) count = 0;
        var below = level.getBlockEntity(worldPosition.below());
        if (status == 2 && count > 50 && (!hasStabilizer() || !(below instanceof EnergizedNodeBlockEntity))) {
            status = 0; count = 50; EnergizedNodeBlock.collapse(level, worldPosition.below());
        } else if (level.hasNeighborSignal(worldPosition) && below instanceof AuraNodeBlockEntity && hasStabilizer()) status = 1;
        else if (below instanceof EnergizedNodeBlockEntity) { status = 2; count = 1000; }
        else status = 0;
        sync();
    }
    public void tick() {
        if (level == null || !surroundingsLoaded()) return;
        if (count < 0) { if (level.isClientSide) count = 0; else checkStatus(); }
        if (!level.isClientSide) {
            var below = level.getBlockEntity(worldPosition.below());
            if (status == 1 && count >= 1000 && below instanceof AuraNodeBlockEntity node) {
                var values = node.permanentBase(); var type = node.permanentType(); var modifier = node.permanentModifier();
                converting = true;
                try {
                    level.setBlock(worldPosition.below(), TCArcane.ENERGIZED_NODE.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(worldPosition.below()) instanceof EnergizedNodeBlockEntity result) result.configure(values, type, modifier, "blank");
                } finally { converting = false; }
                checkStatus(); burst();
            } else if (status == 2 && count <= 50 && below instanceof EnergizedNodeBlockEntity node) {
                var values = node.aura(); var type = node.nodeType(); var modifier = node.nodeModifier();
                converting = true;
                try {
                    level.setBlock(worldPosition.below(), TCArcane.AURA_NODE.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(worldPosition.below()) instanceof AuraNodeBlockEntity result) result.configure(values, type, modifier, "", true);
                } finally { converting = false; }
                status = 0; sync(); burst();
            }
        }
        int previous = count;
        if (status == 0 || !level.hasNeighborSignal(worldPosition)) count = Math.max(0, count - 1);
        else if (count < 1000) {
            count++;
            if (!level.isClientSide && level.getBlockEntity(worldPosition.below()) instanceof AuraNodeBlockEntity node) {
                var aspects = node.aspects().aspects();
                if (!aspects.isEmpty()) node.take(aspects.get(level.random.nextInt(aspects.size())), 1);
                if (count % 5 == 0 || node.aspects().total() == 0) node.sync();
            }
        }
        if (level.isClientSide && previous != count && count > 50)
            tc4revival.client.ClientFocusEffects.nodeConversion(level,worldPosition,hasStabilizer());
        if (!level.isClientSide && previous != count) { setChanged(); if (count % 20 == 0 || count == 50 || count == 1000) sync(); }
    }
    private void burst() {
        var position = Vec3.atCenterOf(worldPosition.below()); FocusEffectPacket.burst(level, position, 1);
        level.playSound(null, worldPosition.below(), TCSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, .5f, 1);
    }
    private void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("count", count); tag.putInt("status", status); }
    @Override public void load(CompoundTag tag) { super.load(tag); count = Math.max(-1, Math.min(1000, tag.getInt("count"))); status = Math.max(0, Math.min(2, tag.getInt("status"))); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
