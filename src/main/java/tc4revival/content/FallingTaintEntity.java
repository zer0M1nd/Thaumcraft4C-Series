package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

public  class FallingTaintEntity extends Entity implements IEntityAdditionalSpawnData {
    private BlockState block=Blocks.AIR.defaultBlockState();
    private BlockPos source=BlockPos.ZERO;
    private int fallTime;
    public FallingTaintEntity(EntityType<? extends FallingTaintEntity> type,Level level) { super(type,level); }
    public void begin(BlockPos source,BlockPos target,BlockState block) {
        this.source=source.immutable(); this.block=block; setPos(target.getX()+.5,target.getY()+.01,target.getZ()+.5);
    }
    public BlockState carriedState() { return block; }
    @Override protected void defineSynchedData() {}
    @Override public boolean canBeCollidedWith() { return !isRemoved(); }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public void tick() {
        if (block.isAir()) { discard(); return; }
        xo=getX(); yo=getY(); zo=getZ(); fallTime++;
        setDeltaMovement(getDeltaMovement().add(0,-.04f,0)); move(MoverType.SELF,getDeltaMovement()); setDeltaMovement(getDeltaMovement().scale(.98f));
        if (!(level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (fallTime==1) {
            if (!level.hasChunkAt(source) || !level.getBlockState(source).is(block.getBlock())) { discard(); return; }
            level.removeBlock(source,false);
        }
        var pos=BlockPos.containing(getX(),getY()+.49,getZ()); var below=level.getBlockState(pos.below());
        if (onGround() || (below.getBlock() instanceof FluxGooBlock && below.getValue(FiniteFluidBlock.QUANTA)>=5)) {
            setDeltaMovement(getDeltaMovement().multiply(.7,-.5,.7));
            var old=level.getBlockState(pos);
            if (old.is(Blocks.PISTON) || old.is(Blocks.STICKY_PISTON) || old.is(Blocks.PISTON_HEAD) || old.is(Blocks.MOVING_PISTON)) return;
            playSound(TCSounds.GORE.get(),.5f,((random.nextFloat()-random.nextFloat())*.2f+1)*.8f); discard();
            boolean replace=old.getBlock() instanceof TaintFibreBlock || old.getBlock() instanceof FluxGooBlock || old.canBeReplaced();
            if (replace && level.getBlockEntity(pos)==null && !TaintSolidBlock.canFallThrough(level,pos.below()) && !level.isOutsideBuildHeight(pos)) level.setBlock(pos,block,3);
        } else if ((fallTime>100 && level.isOutsideBuildHeight(pos)) || fallTime>600) discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState",NbtUtils.writeBlockState(block)); tag.putLong("Source",source.asLong()); tag.putByte("Time",(byte)fallTime);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        block=NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK),tag.getCompound("BlockState"));
        source=BlockPos.of(tag.getLong("Source")); fallTime=tag.getByte("Time")&255;
        if (!(block.getBlock() instanceof TaintSolidBlock)) block=Blocks.AIR.defaultBlockState();
    }
    @Override public void writeSpawnData(FriendlyByteBuf buffer) { buffer.writeVarInt(Block.getId(block)); }
    @Override public void readSpawnData(FriendlyByteBuf buffer) { block=Block.stateById(buffer.readVarInt()); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
