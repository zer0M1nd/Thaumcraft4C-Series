package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public  class InfusionPillarBlockEntity extends BlockEntity {
    private int orientation;
    public InfusionPillarBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.INFUSION_PILLAR_ENTITY.get(),pos,state); }
    protected InfusionPillarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public int orientation() { return orientation; }
    public void orient(int value) {
        orientation=value; setChanged();
        if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putByte("orientation",(byte)orientation); }
    @Override public void load(CompoundTag tag) { super.load(tag); orientation=tag.getByte("orientation"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0,1,0).inflate(1,0,1); }
}
