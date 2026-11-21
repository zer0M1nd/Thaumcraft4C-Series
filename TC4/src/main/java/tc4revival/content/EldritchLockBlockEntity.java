package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public  class EldritchLockBlockEntity extends BlockEntity {
    private int count=-1;
    private int encounterKind=-1;
    private long retryAfter;
    public EldritchLockBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.LOCK_ENTITY.get(),pos,state); }
    public int count() { return count; }
    public boolean activate() {
        if (!(level instanceof ServerLevel server) || count>=0) return false;
        count=0; sync(); server.playSound(null,worldPosition,TCSounds.RUNIC_SHIELD_CHARGE.get(),SoundSource.BLOCKS,1,1); return true;
    }
    public void tick() {
        if (level==null || count<0) return;
        if (level instanceof ServerLevel && level.getGameTime()<retryAfter) return;
        count++;
        if (count%5==0 && !level.isClientSide) level.playSound(null,worldPosition,TCSounds.PUMP.get(),SoundSource.BLOCKS,1,1);
        if (level instanceof ServerLevel server) {
            setChanged();
            if (count>100) {
                try {
                    if (encounterKind<0) { encounterKind=EldritchMazeSavedData.get(server).bossAt(worldPosition.getX()>>4,worldPosition.getZ()>>4); setChanged(); }
                    EldritchBossEncounter.open(server,worldPosition,encounterKind);
                    server.playSound(null,worldPosition,TCSounds.ICE.get(),SoundSource.BLOCKS,1,1);
                } catch (RuntimeException failure) {
                    count=100; retryAfter=level.getGameTime()+200; setChanged();
                    // Opening rollback may have recreated this block entity.
                    if (level.getBlockEntity(worldPosition) instanceof EldritchLockBlockEntity restored && restored!=this) {
                        restored.count=count; restored.encounterKind=encounterKind; restored.retryAfter=retryAfter; restored.setChanged();
                    }
                    com.mojang.logging.LogUtils.getLogger().error("Eldritch lock encounter failed at {}; retaining selected boss and retrying in 200 ticks",worldPosition,failure);
                }
            }
        }
    }
    private void sync() { setChanged(); if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putShort("count",(short)count); tag.putInt("EncounterKind",encounterKind); }
    @Override public void load(CompoundTag tag) {
        super.load(tag); count=tag.contains("count") ? Math.max(-1,tag.getShort("count")) : -1;
        encounterKind=tag.contains("EncounterKind") ? tag.getInt("EncounterKind") : -1;
        if (encounterKind< -1 || encounterKind>3) encounterKind=-1;
        retryAfter=0;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(2.25); }
}
