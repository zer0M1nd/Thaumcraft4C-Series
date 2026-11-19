package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class OwnedDeviceBlockEntity extends BlockEntity {
    private final OwnedDeviceAccess access=new OwnedDeviceAccess();
    public OwnedDeviceBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.OWNED_DEVICE_ENTITY.get(),pos,state); }
    public OwnedDeviceAccess access() { return access; }
    public void changed() {
        setChanged();
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); access.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); access.load(tag); }
    @Override public CompoundTag getUpdateTag() {
        var tag=new CompoundTag(); tag.putString("owner",access.owner()); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
