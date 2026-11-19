package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public  class EldritchCrabSpawnerBlockEntity extends BlockEntity {
    private int countdown=150,venting;
    private Direction facing=Direction.DOWN;
    public EldritchCrabSpawnerBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.CRAB_SPAWNER_ENTITY.get(),pos,state); }
    public Direction facing() { return facing; }
    public void setFacing(Direction value) {
        facing=java.util.Objects.requireNonNull(value); setChanged();
        if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    public void tick() {
        if (level==null) return;
        if (level.isClientSide) {
            for(int count=advanceClientVenting(level.random);count>0;count--)vent();
            return;
        }
        countdown--;
        if (countdown<0) { countdown=50+level.random.nextInt(50); return; }
        if ((countdown==15 || countdown==0) && activated() && level.getEntitiesOfClass(EldritchCrabEntity.class,new AABB(worldPosition).inflate(32)).size()<=5) {
            if (countdown==15) {
                level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);
                level.playSound(null,worldPosition,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.5f,1);
            } else {
                countdown=150+level.random.nextInt(100); var crab=TCEntities.ELDRITCH_CRAB.get().create(level);
                if (crab!=null) {
                    BlockPos spawn=worldPosition.relative(facing); crab.moveTo(spawn.getX()+.5,spawn.getY()+.5,spawn.getZ()+.5,0,0);
                    crab.finalizeSpawn((ServerLevel)level,level.getCurrentDifficultyAt(spawn),MobSpawnType.SPAWNER,null,null);
                    crab.setHelm(false); crab.setDeltaMovement(facing.getStepX()*.2,facing.getStepY()*.2,facing.getStepZ()*.2);
                    level.addFreshEntity(crab);
                }
                level.playSound(null,worldPosition,TCSounds.GORE.get(),SoundSource.BLOCKS,.5f,1);
            }
        }
    }
    public boolean activated() { return level!=null && level.getNearestPlayer(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,16,false)!=null; }
    /** Shared scheduler for the legacy event burst and idle smoke; no client classes needed. */
    public int advanceClientVenting(net.minecraft.util.RandomSource random) {
        if(venting>0){venting--;return 3;}
        return random.nextInt(20)==0?1:0;
    }
    private void vent() {
        var random=level.random;
        float x=.15f-random.nextFloat()*.3f,z=.15f-random.nextFloat()*.3f,y=.15f-random.nextFloat()*.3f;
        float dx=.1f-random.nextFloat()*.2f,dz=.1f-random.nextFloat()*.2f,dy=.1f-random.nextFloat()*.2f;
        tc4revival.client.EssentiaVentParticles.emit(level,worldPosition.getX()+.5+x+facing.getStepX()/2.1f,
                worldPosition.getY()+.5+y+facing.getStepY()/2.1f,worldPosition.getZ()+.5+z+facing.getStepZ()/2.1f,
                facing.getStepX()/3f+dx,facing.getStepY()/3f+dy,facing.getStepZ()/3f+dz,0x9988aa,2);
    }
    @Override public boolean triggerEvent(int id,int parameter) { if (id==1) { venting=20; return true; } return super.triggerEvent(id,parameter); }
    @Override public void load(CompoundTag tag) { super.load(tag); int direction=tag.getByte("facing"); facing=direction>=0 && direction<6 ? Direction.from3DDataValue(direction) : Direction.DOWN; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putByte("facing",(byte)facing.get3DDataValue()); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1); }
}
