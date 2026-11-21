package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;
public final class Samurai extends ConciliumHostile {
 private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(Samurai.class,EntityDataSerializers.INT),ANGER=SynchedEntityData.defineId(Samurai.class,EntityDataSerializers.INT);
 public Samurai(EntityType<? extends Samurai> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,10);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);entityData.define(ANGER,0);}
 public int samuraiType(){return entityData.get(TYPE);}public void setSamuraiType(int type){entityData.set(TYPE,net.minecraft.util.Mth.clamp(type,0,2));}public int anger(){return entityData.get(ANGER);}private void anger(int value){entityData.set(ANGER,value);}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){var result=super.finalizeSpawn(level,difficulty,reason,group,tag);setSamuraiType(random.nextInt(3));return result;}
 @Override public boolean isAlliedTo(Entity other){return other instanceof Samurai||super.isAlliedTo(other);}
 @Override public int getArmorValue(){return 20;}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return amount/(samuraiType()==2?10:samuraiType()==1?5:3.5f);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new Attack());goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,1));goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targets();}
 @Override public boolean doHurtTarget(Entity victim){float damage=(float)getAttributeValue(Attributes.ATTACK_DAMAGE)+(samuraiType()==2?15:samuraiType()==1?10:7);int knockback=EnchantmentHelper.getKnockbackBonus(this);if(victim instanceof LivingEntity living)damage+=EnchantmentHelper.getDamageBonus(getMainHandItem(),living.getMobType());boolean hit=victim.hurt(damageSources().mobAttack(this),damage);if(hit){if(knockback>0){victim.push(-Math.sin(Math.toRadians(getYRot()))*knockback*.5,.1,Math.cos(Math.toRadians(getYRot()))*knockback*.5);setDeltaMovement(getDeltaMovement().multiply(.6,1,.6));}int fire=EnchantmentHelper.getFireAspect(this);if(fire>0)victim.setSecondsOnFire(fire*4);doEnchantDamageEffects(this,victim);setLastHurtMob(victim);}return hit;}
 private final class Attack extends Goal {
  private int melee,ranged,strafe;private int direction=1;
  Attack(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
  @Override public boolean canUse(){return getTarget()!=null&&getTarget().isAlive()&&distanceToSqr(getTarget())<=1000;}
  @Override public boolean canContinueToUse(){return canUse();}
  @Override public boolean requiresUpdateEveryTick(){return true;}
  @Override public void start(){melee=ranged=0;if(getHealth()>getMaxHealth()/3)anger(1);}
  @Override public void stop(){anger(0);getNavigation().stop();}
  @Override public void tick(){var target=getTarget();if(target==null)return;Vec3 delta=target.position().subtract(position());double distance=delta.horizontalDistanceSqr();Vec3 forward=new Vec3(delta.x,0,delta.z).normalize();float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90);setYRot(yaw);yHeadRot=yBodyRot=yaw;
   if(getHealth()>getMaxHealth()/3){anger(1);if(getHealth()>getMaxHealth()/2&&distance>64&&random.nextInt(10)>7)addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,10,3));setDeltaMovement(forward.x*.25,onGround()?0:getDeltaMovement().y,forward.z*.25);getNavigation().moveTo(target,.25);if(melee>0)melee--;if(distance<=4&&delta.y*delta.y<=4&&melee<=0){swing(InteractionHand.MAIN_HAND);doHurtTarget(target);melee=20;}return;}
   anger(1);if(distance<340){if(strafe--<=0){strafe=40;direction=random.nextBoolean()?1:-1;}setDeltaMovement(-forward.x*.25+forward.z*.1*direction,onGround()?0:getDeltaMovement().y,-forward.z*.25-forward.x*.1*direction);getNavigation().stop();}else getNavigation().moveTo(getX()-forward.x*6,getY(),getZ()-forward.z*6,.25);
   if(ranged<40)ranged++;if(ranged>=40&&distance<=400&&getSensing().hasLineOfSight(target)){playSound(TCSounds.SHOCK.get(),.25f,1);target.hurt(damageSources().mobAttack(Samurai.this),2*(samuraiType()+1));ConciliumArcNetwork.emit(level(),position().add(0,getBbHeight()-.5,0),target.position().add(0,target.getBbHeight()/2,0),0x6666dd,.02f);ranged=0;}
  }
 }
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return RemainingMobs.SPEECH.get();}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return RemainingMobs.UAGH.get();}
 @Override protected float getSoundVolume(){return .5f;}
 @Override public int getAmbientSoundInterval(){return 450;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){if(random.nextInt(10)%3==0)spawnAtLocation(new ItemStack(TCItems.bauble(MundaneBaubleType.values()[random.nextInt(3)])),1.5f);Item item=samuraiType()==0?TCItems.nugget(NuggetType.THAUMIUM):samuraiType()==1?TCItems.nugget(NuggetType.VOID):net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("taintedmagic:shadowmetal_nugget"));if(item!=null&&item!=Items.AIR)spawnAtLocation(new ItemStack(item,4+random.nextInt(10)),1.5f);super.dropCustomDeathLoot(source,looting,hit);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Type",samuraiType());tag.putInt("Anger",anger());}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setSamuraiType(tag.getInt("Type"));anger(net.minecraft.util.Mth.clamp(tag.getInt("Anger"),0,1));}
}
