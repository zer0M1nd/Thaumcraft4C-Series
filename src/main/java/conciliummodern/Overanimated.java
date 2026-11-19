package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
public final class Overanimated extends ConciliumHostile {
 private int spawnedPieces;
 public Overanimated(EntityType<? extends Overanimated> type,Level level){super(type,level);xpReward=0;}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,5);}
 @Override protected void registerGoals(){basicGoals(.6,false);}
 @Override protected void targets(){legacyTargets(true,true,false,false);}
 @Override protected boolean shouldDropLoot(){return false;}
 @Override protected void tickDeath(){if(level().isClientSide)return;deathTime++;if(deathTime!=1&&deathTime%20!=0)return;
 for(int i=0;i<6;i++)if((spawnedPieces&(1<<i))==0){var gib=RemainingMobs.THAUM_GIB.get().create(level());if(gib==null)continue;gib.setGibType(i);gib.moveTo(getX(),getY()+(i==0?1.5:i<4?1:0),getZ(),getYRot(),0);Vec3 v=new Vec3(random.nextDouble()-.5,random.nextDouble()*.5,random.nextDouble()-.5);gib.setDeltaMovement(v.lengthSqr()<1e-8?new Vec3(0,.5,0):v.normalize().scale(.5));if(level().addFreshEntity(gib))spawnedPieces|=1<<i;}
 if(spawnedPieces==63){ConciliumArcNetwork.blood(level(),position());discard();}}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return RemainingMobs.GARGLE_MANY.get();}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return RemainingMobs.GARGLE_MANY.get();}
 @Override protected float getSoundVolume(){return .6f;}
 @Override public int getAmbientSoundInterval(){return 65;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("SpawnedPieces",spawnedPieces);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);spawnedPieces=tag.getInt("SpawnedPieces")&63;}
}
