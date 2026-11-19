package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Original orbiting broom witch; brew effects remain owned by the actual Witchery host. */
public final class BurnedWitch extends FlyingMob implements Enemy {
 private double orbitAngle,orbitRadius;private Vec3 waypoint=Vec3.ZERO;private int courseCooldown,aggroCooldown,attackCounter;
 public BurnedWitch(EntityType<? extends BurnedWitch> type,Level level){super(type,level);xpReward=10;setNoGravity(true);}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.FOLLOW_RANGE,100);}
 @Override protected boolean shouldDespawnInPeaceful(){return true;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
 @Override public boolean isIgnoringBlockTriggers(){return true;}
 @Override public int getMaxSpawnClusterSize(){return 1;}
 @Override protected void customServerAiStep(){if(!WitcheryBridge.available()){setTarget(null);setDeltaMovement(Vec3.ZERO);return;}if(level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}var host=WitcheryBridge.host().orElseThrow();var gas=host.gasMask();if(gas!=null&&!hasEffect(gas))addEffect(new MobEffectInstance(gas,65536,1));
 if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);if(getTarget()==null||aggroCooldown--<=0){Player nearest=null;double best=10000;for(var player:level().players())if(player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&distanceToSqr(player)<best){nearest=player;best=distanceToSqr(player);}setTarget(nearest);if(nearest!=null){aggroCooldown=20;orbitRadius=4+random.nextDouble()*4;orbitAngle=random.nextDouble()*Math.PI*2;}}
 var target=getTarget();if(target!=null){orbitAngle+=.06;waypoint=target.position().add(Math.cos(orbitAngle)*orbitRadius,8+Math.sin(orbitAngle*2)*.6,Math.sin(orbitAngle)*orbitRadius);if(courseCooldown>3)courseCooldown=0;}else{double distance=waypoint.distanceToSqr(position());if(distance<1||distance>3600)waypoint=position().add((random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16);}
 var delta=waypoint.subtract(position());double length=delta.length();if(courseCooldown--<=0){courseCooldown+=random.nextInt(5)+2;if(length>1e-8&&traversable(delta,length))setDeltaMovement(getDeltaMovement().add(delta.scale(.1/length)));else waypoint=position();}
 if(target!=null&&distanceToSqr(target)<4096){setYRot((float)-Math.toDegrees(Math.atan2(target.getX()-getX(),target.getZ()-getZ())));yBodyRot=getYRot();if(hasLineOfSight(target)){if(attackCounter==10)level().levelEvent(1015,blockPosition(),0);if(++attackCounter==10){if(target instanceof Player player){int roll=random.nextInt(3);String brew=roll==0?"ill_fitting":roll==1&&host.vampire(player)?"attack_vampire":roll==1&&host.werewolfLevel(player)>0?"attack_werewolf":"attack";throwBrew(brew,target);}attackCounter=-20;}}else if(attackCounter>0)attackCounter--;}else{setYRot((float)-Math.toDegrees(Math.atan2(getDeltaMovement().x,getDeltaMovement().z)));yBodyRot=getYRot();if(attackCounter>0)attackCounter--;}}
 private boolean traversable(Vec3 delta,double length){Vec3 step=delta.scale(1/length);var box=getBoundingBox();for(int i=1;i<Math.min(128,length);i++){box=box.move(step);if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(box.getCenter()))||!level().getWorldBorder().isWithinBounds(box)||!level().noCollision(this,box))return false;}return true;}
 private void throwBrew(String name,LivingEntity target){if(!(level() instanceof ServerLevel level))return;var host=WitcheryBridge.host().orElseThrow();ItemStack brew=WitcheryBridge.brew(name);if(brew.isEmpty())throw new IllegalStateException("Witchery host returned empty brew "+name);var projectile=host.brewProjectile(level,this,brew);if(projectile==null)throw new IllegalStateException("Witchery host returned no brew projectile");projectile.setOwner(this);double x=target.getX()-getX(),z=target.getZ()-getZ(),y=target.getEyeY()-1.1-projectile.getY();projectile.shoot(x,y+Math.sqrt(x*x+z*z)*.2,z,.75f,8);level.addFreshEntity(projectile);}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return net.minecraft.sounds.SoundEvents.GHAST_AMBIENT;}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.GHAST_HURT;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.GHAST_DEATH;}
 @Override public float getVoicePitch(){return 1.5f;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){if(WitcheryBridge.available())for(int i=0,total=1+random.nextInt(3);i<total;i++){int count=random.nextInt(3)+(looting>0?random.nextInt(looting+1):0),which=random.nextInt(5);var stack=which==0?new ItemStack(Items.BLAZE_POWDER):which==1?new ItemStack(Items.GHAST_TEAR):WitcheryBridge.item("generic",new int[]{28,30,35}[which-2]);if(!stack.isEmpty()&&count>0){stack=stack.copy();stack.setCount(count);spawnAtLocation(stack,1.5f);}}super.dropCustomDeathLoot(source,looting,hit);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putDouble("OrbitAngle",orbitAngle);tag.putDouble("OrbitRadius",orbitRadius);tag.putInt("AttackCounter",attackCounter);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);double angle=tag.getDouble("OrbitAngle"),radius=tag.getDouble("OrbitRadius");orbitAngle=Double.isFinite(angle)?angle:0;orbitRadius=Double.isFinite(radius)?net.minecraft.util.Mth.clamp(radius,4,8):4;attackCounter=net.minecraft.util.Mth.clamp(tag.getInt("AttackCounter"),-20,10);waypoint=position();}
}
