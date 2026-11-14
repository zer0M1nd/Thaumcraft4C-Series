package advancedthaumaturgy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.nbt.CompoundTag;
public class SyncedEntity extends BlockEntity {
    public SyncedEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
}
