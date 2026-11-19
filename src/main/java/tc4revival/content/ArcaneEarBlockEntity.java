package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class ArcaneEarBlockEntity extends BlockEntity {
    private byte note,tone;
    private int signal;
    private boolean restored;
    private long heardAt=Long.MIN_VALUE;
    public ArcaneEarBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ARCANE_EAR_ENTITY.get(),pos,state); }
    public int note() { return note; }
    public int tone() { return tone; }
    public int signalTicks() { return signal; }
    @Override public void onLoad() { super.onLoad(); if (level!=null && !level.isClientSide) { ArcaneEarListening.register(this); updateTone(); } }
    @Override public void setRemoved() { ArcaneEarListening.remove(this); super.setRemoved(); }
    public void updateTone() { if (level!=null) { tone=(byte)ArcaneEarListening.tone(level.getBlockState(worldPosition.below())); setChanged(); } }
    public void changePitch() { note=(byte)((note+1)%25); sync(); trigger(true); }
    public void hear(int instrument,int pitch,BlockPos source) {
        if (level==null || instrument!=tone || pitch!=note || worldPosition.distSqr(source)>4096 || heardAt==level.getGameTime()) return;
        heardAt=level.getGameTime(); signal=10; trigger(false); setSignal(true);
    }
    public void trigger(boolean sound) {
        if (level!=null && level.isEmptyBlock(worldPosition.above())) level.blockEvent(worldPosition,getBlockState().getBlock(),sound ? ArcaneEarListening.tone(level.getBlockState(worldPosition.below())) : -1,note);
    }
    private void setSignal(boolean powered) {
        if (level==null) return;
        var state=getBlockState(); level.setBlock(worldPosition,state.setValue(ArcaneEarBlock.POWERED,powered),2);
        level.updateNeighborsAt(worldPosition,state.getBlock()); level.updateNeighborsAt(worldPosition.below(),state.getBlock());
    }
    private void sync() { setChanged(); if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    public static void tick(Level level,BlockPos pos,BlockState state,ArcaneEarBlockEntity ear) {
        if (ear.signal>0 && ear.heardAt!=level.getGameTime()) {
            if (--ear.signal==0) ear.setSignal(false); ear.setChanged();
        }
        // Old saves without Signal still clear stale powered state.
        else if (ear.signal==0 && state.getValue(ArcaneEarBlock.POWERED)) ear.setSignal(false);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putByte("note",note); tag.putByte("tone",tone); tag.putInt("Signal",signal); }
    @Override public void load(CompoundTag tag) { super.load(tag); restored=true; note=(byte)Math.max(0,Math.min(24,tag.getByte("note"))); tone=tag.getByte("tone"); signal=Math.max(0,Math.min(10,tag.getInt("Signal"))); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
