package automagy.content;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
/** Standalone fire spirit; it does not require inheriting the base mod's final WispEntity. */
public final class NetherWispEntity extends FlyingMob implements Enemy {
 private static final net.minecraft.network.syncher.EntityDataAccessor<String> ASPECT=net.minecraft.network.syncher.SynchedEntityData.defineId(NetherWispEntity.class,net.minecraft.network.syncher.EntityDataSerializers.STRING);
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ASPECT,"");}
 public tc4revival.aspect.Aspect aspect(){return tc4revival.aspect.TCAspects.catalog().find(entityData.get(ASPECT)).orElseGet(()->tc4revival.aspect.TCAspects.catalog().require("ignis"));}
 private void chooseAspect(){if(!level().isClientSide&&tc4revival.aspect.TCAspects.catalog().find(entityData.get(ASPECT)).isEmpty())entityData.set(ASPECT,automagy.OptionalAspects.nether().filter(a->random.nextInt(4)==0).map(tc4revival.aspect.Aspect::tag).orElse("ignis"));}
 @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putString("Aspect",entityData.get(ASPECT));var entry=new net.minecraft.nbt.CompoundTag();entry.putString("key",aspect().tag());entry.putInt("amount",2);var list=new net.minecraft.nbt.ListTag();list.add(entry);tag.put("Essence",list);}
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(ASPECT,tag.getString("Aspect"));}
 private Vec3 destination=Vec3.ZERO;private int steer,attack;
 public NetherWispEntity(EntityType<? extends NetherWispEntity> type,Level level){super(type,level);xpReward=6;}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5);}
 @Override protected int decreaseAirSupply(int air){return air;}
 @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
 @Override protected boolean shouldDespawnInPeaceful(){return true;}
 @Override public boolean removeWhenFarAway(double distance){return true;}
 @Override public int getMaxSpawnClusterSize(){return 2;}
 @Override public boolean hurt(DamageSource source,float amount){if(!level().isClientSide&&source.getEntity() instanceof LivingEntity attacker)setTarget(attacker);return super.hurt(source,amount);}
 @Override protected void customServerAiStep(){if(level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);if(tickCount%60==0&&getTarget()==null)setTarget(level().getNearestPlayer(getX(),getY(),getZ(),16,true));
  Vec3 offset=destination.subtract(position());double distance=offset.length();if(distance<1||distance>60)destination=position().add((random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16);
  if(steer--<=0){steer=random.nextInt(5)+2;if(distance>1e-8&&canDrift(destination))setDeltaMovement(getDeltaMovement().add(offset.scale(.1/distance)));else destination=position();}
  var target=getTarget();if(target!=null&&distanceToSqr(target)<256){setYRot((float)(-Math.atan2(target.getX()-getX(),target.getZ()-getZ())*180/Math.PI));yBodyRot=getYRot();if(hasLineOfSight(target)){if(++attack==20)zap(target);}else if(attack>0)attack--;}
  else {setYRot((float)(-Math.atan2(getDeltaMovement().x,getDeltaMovement().z)*180/Math.PI));yBodyRot=getYRot();if(attack>0)attack--;}
 }
 private boolean canDrift(Vec3 target){Vec3 path=target.subtract(position());double length=path.length();if(length<1e-8)return false;Vec3 step=path.scale(1/length);var box=getBoundingBox();for(int i=1;i<length;i++){box=box.move(step);if(!level().hasChunkAt(BlockPos.containing(box.getCenter()))||!level().noCollision(this,box))return false;}BlockPos p=BlockPos.containing(target);if(!level().hasChunkAt(p)||!level().getFluidState(p).isEmpty())return false;for(int i=0;i<11;i++)if(!level().isEmptyBlock(p.below(i)))return true;return false;}
 private void zap(LivingEntity target){level().playSound(null,blockPosition(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.HOSTILE,1,1.1f);tc4revival.network.FocusEffectPacket.lightning(level(),this,target);var movement=target.getDeltaMovement();boolean moving=Math.abs(movement.x)>.1||Math.abs(movement.y)>.1||Math.abs(movement.z)>.1;if(random.nextFloat()<(moving?.4f:.66f))target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)+(moving?0:1));attack=-20+random.nextInt(20);}
 @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide&&!getPersistentData().getBoolean("AutomagyDeathBurst")){getPersistentData().putBoolean("AutomagyDeathBurst",true);level().explode(this,getX(),getY(),getZ(),1.5f,true,Level.ExplosionInteraction.NONE);}}
 @Override public void tick(){chooseAspect();super.tick();if(level().isClientSide)automagy.client.NetherWispRenderer.effects(this);if(level().isClientSide&&tickCount%5==0){level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,getX(),getY()+.45,getZ(),0,0,0);level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY()+.45,getZ(),0,0,0);}}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return tc4revival.content.TCSounds.WISP_LIVE.get();}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return tc4revival.content.TCSounds.WISP_DEAD.get();}
 @Override protected float getSoundVolume(){return .25f;}
}
