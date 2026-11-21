package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PlatformEntity extends BlockEntity {
    private BlockState cover;
    public PlatformEntity(BlockPos pos,BlockState state) { super(Tinkerer.PLATFORM_ENTITY.get(),pos,state); }
    public BlockState cover() { return cover; }
    public void cover(BlockState next) { cover=next;setChanged();if(level!=null){level.setBlock(worldPosition,getBlockState().setValue(InterfaceBlock.CAMOUFLAGED,next!=null),3);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);} }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);if(cover!=null)tag.put("Cover",NbtUtils.writeBlockState(cover)); }
    @Override public void load(CompoundTag tag) { super.load(tag);cover=tag.contains("Cover",Tag.TAG_COMPOUND)?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("Cover")):null; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
}
