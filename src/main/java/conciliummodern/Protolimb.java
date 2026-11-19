package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Protolimb extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(Protolimb.class,EntityDataSerializers.INT);
    private Vec3 destination;
    public Protolimb(EntityType<? extends Protolimb> type,Level level){super(type,level);xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);}
    public int limbType(){return entityData.get(TYPE);}
    public void setLimbType(int type){entityData.set(TYPE,Mth.clamp(type,0,5));}
    @Override public void tick(){
        if(!level().isClientSide && isAlive()){
            if(destination==null || random.nextInt(100)==0 || destination.distanceToSqr(position())<3)
                destination=position().add((random.nextDouble()-.5)*16,(random.nextDouble()-.5)*8,(random.nextDouble()-.5)*16);
            var player=level().getNearestPlayer(this,16);
            if(player!=null && !player.isSpectator()){
                double dx=getX()-player.getX(),dz=getZ()-player.getZ(),distance=Math.sqrt(dx*dx+dz*dz);
                if(distance<4 && distance>1e-6)destination=destination.add(dx/distance*4,(getY()-player.getY())*.5,dz/distance*4);
            }
            var delta=destination.subtract(position());var old=getDeltaMovement();
            setDeltaMovement(old.add((Math.signum(delta.x)*.1-old.x)*.1,(Math.signum(delta.y)*.1-old.y)*.1,(Math.signum(delta.z)*.1-old.z)*.1));
        }
        super.tick();
        if(!level().isClientSide && isAlive())for(var player:level().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(.1)))if(!player.isSpectator()){playerTouch(player);break;}
    }
    @Override public void travel(Vec3 input){move(MoverType.SELF,getDeltaMovement());}
    @Override public void playerTouch(Player player){
        if(!(player instanceof ServerPlayer server)||!isAlive()||player.isSpectator())return;
        EtherealState.addLimb(server);discard();
        level().playSound(null,blockPosition(),tc4revival.content.TCSounds.CRAFT_FAIL.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
    }
    @Override public boolean canPickUpLoot(){return false;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Type",limbType());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setLimbType(tag.getInt("Type"));destination=null;}
}
