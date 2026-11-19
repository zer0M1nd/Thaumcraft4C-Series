package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Capacity-limited experience storage; excess orb points remain in the world. */
public  class BrainJarBlockEntity extends BlockEntity {
    public static final int CAPACITY=2000;
    private int xp;
    public int eatDelay;
    public float rotation,previousRotation,targetRotation;
    private long nextSigh=System.currentTimeMillis()+1500;
    public BrainJarBlockEntity(BlockPos pos,BlockState state){super(TCArcane.BRAIN_JAR_ENTITY.get(),pos,state);}
    public int experience(){return xp;}
    public int experienceCapacity(){return CAPACITY;}
    /** Returns accepted points; simulation has no side effects, including cooldown or orbs. */
    public int insertExperience(int amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative experience transfer");
        if (!thaumcraft.api.LiveBlockEntity.server(this)) return 0;
        int accepted = Math.min(amount, Math.max(0, experienceCapacity() - xp));
        if (!simulate && accepted > 0) { xp += accepted; changed(); }
        return accepted;
    }
    /** Returns extracted points without spawning orbs or changing eatDelay. */
    public int extractExperience(int amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative experience transfer");
        if (!thaumcraft.api.LiveBlockEntity.server(this)) return 0;
        int extracted = Math.min(amount, xp);
        if (!simulate && extracted > 0) { xp -= extracted; changed(); }
        return extracted;
    }
    public int comparator(){return Math.min(15,(int)(xp/(float)CAPACITY*14)+(xp>0?1:0));}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    public ExperienceOrb closestOrb(){
        if(level==null)return null;return level.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(6),Entity::isAlive).stream()
                .min(java.util.Comparator.comparingDouble(orb->orb.position().distanceToSqr(Vec3.atCenterOf(worldPosition)))).orElse(null);
    }
    static int orbExperience(ExperienceOrb orb){var tag=new CompoundTag();orb.addAdditionalSaveData(tag);return (int)Math.min(Integer.MAX_VALUE,(long)Math.max(0,orb.getValue())*Math.max(1,tag.getInt("Count")));}
    public void tick(){
        if(level==null)return;
        Entity focus=xp<CAPACITY?closestOrb():null;
        if(focus!=null&&eatDelay==0){
            var delta=Vec3.atCenterOf(worldPosition).subtract(focus.position()).scale(1/7d);double distance=delta.length(),strength=1-distance;
            if(distance>1e-8&&strength>0){strength*=strength;focus.setDeltaMovement(focus.getDeltaMovement().add(delta.x/distance*strength*.15,delta.y/distance*strength*.33,delta.z/distance*strength*.15));}
        }
        if(level.isClientSide){
            previousRotation=rotation;
            if(focus==null){focus=level.getNearestPlayer(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,6,false);
                if(focus!=null&&System.currentTimeMillis()>nextSigh){level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,TCSounds.BRAIN.get(),net.minecraft.sounds.SoundSource.BLOCKS,.15f,.8f+level.random.nextFloat()*.4f,false);nextSigh=System.currentTimeMillis()+5000+level.random.nextInt(25000);}}
            if(focus==null)targetRotation+=.01f;else targetRotation=(float)Math.atan2(focus.getZ()-worldPosition.getZ()-.5,focus.getX()-worldPosition.getX()-.5);
            rotation=wrap(rotation);targetRotation=wrap(targetRotation);rotation+=wrap(targetRotation-rotation)*.04f;
        }
        if(eatDelay>0){eatDelay--;return;}
        if(!level.isClientSide&&xp<CAPACITY){
            boolean ate=false;
            for(var orb:level.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(.1),Entity::isAlive)){
                int accepted=consumeOrb(orb,Math.max(0,CAPACITY-xp));
                if(accepted==0)continue;
                xp+=accepted;ate=true;
                level.playSound(null,orb.getX(),orb.getY(),orb.getZ(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.BLOCKS,.1f,(level.random.nextFloat()-level.random.nextFloat())*.2f+1);
            }if(ate)changed();
        }
    }
    private int consumeOrb(ExperienceOrb orb,int maximum) {
        int value=orb.getValue();
        if(maximum<=0 || value<=0)return 0;
        var data=new CompoundTag();orb.addAdditionalSaveData(data);
        int count=Math.max(1,data.getInt("Count"));
        int accepted=(int)Math.min(maximum,(long)value*count);
        long remaining=(long)value*count-accepted;
        if(remaining==0){orb.discard();return accepted;}
        int whole=(int)(remaining/value),partial=(int)(remaining%value);
        if(whole>0 && partial>0) {
            var remainder=new ExperienceOrb(level,orb.getX(),orb.getY(),orb.getZ(),partial);
            if(!level.addFreshEntity(remainder))return 0;
        }
        data.putInt("Count",Math.max(1,whole));
        data.putShort("Value",(short)(whole>0?value:partial));
        orb.readAdditionalSaveData(data);
        return accepted;
    }
    public int release(){
        eatDelay=40;if(level==null||level.isClientSide)return 0;
        int requested=level.random.nextInt(Math.min(xp,63)+1),emitted=emit(requested,Vec3.atCenterOf(worldPosition));
        xp-=emitted;if(emitted>0)changed();return emitted;
    }
    private int emit(int amount,Vec3 pos){int emitted=0;
        while(amount>0){int split=ExperienceOrb.getExperienceValue(amount);var orb=new ExperienceOrb(level,pos.x,pos.y,pos.z,split);if(!level.addFreshEntity(orb))break;emitted+=split;amount-=split;}
        return emitted;
    }
    public void broken(){if(level!=null&&!level.isClientSide){xp-=emit(xp,Vec3.atLowerCornerOf(worldPosition));changed();}}
    private static float wrap(float angle){while(angle>=(float)Math.PI)angle-=(float)(Math.PI*2);while(angle<-(float)Math.PI)angle+=(float)(Math.PI*2);return angle;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("XP",xp);}
    @Override public void load(CompoundTag tag){super.load(tag);xp=Math.max(0,tag.getInt("XP"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
