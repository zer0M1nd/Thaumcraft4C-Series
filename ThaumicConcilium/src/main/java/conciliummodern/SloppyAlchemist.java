package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
public final class SloppyAlchemist extends Monster {
 private static final EntityDataAccessor<Boolean> CAPTIVE=SynchedEntityData.defineId(SloppyAlchemist.class,EntityDataSerializers.BOOLEAN);private boolean wasFreed,spawnedRider;
 public SloppyAlchemist(EntityType<? extends SloppyAlchemist> type,Level level){super(type,level);setMaxUpStep(1);setCanPickUpLoot(false);}
 public static AttributeSupplier.Builder attributes(){return Chort.attributes();}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CAPTIVE,false);}
 public boolean captive(){return entityData.get(CAPTIVE);}
 @Override public EntityDimensions getDimensions(Pose pose){return captive()?EntityDimensions.scalable(1,.8f):EntityDimensions.scalable(.5f,1.8f);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key==CAPTIVE)refreshDimensions();}
 @Override public double getPassengersRidingOffset(){return .3;}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new Charge());goalSelector.addGoal(4,new RandomStrollGoal(this,1));goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(5,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,t->Chort.target(this,t)));}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){var result=super.finalizeSpawn(level,difficulty,reason,group,tag);if(!spawnedRider&&WitcheryBridge.alchemistAvailable()){var rider=RemainingMobs.CHORT.get().create(level.getLevel());if(rider!=null){rider.moveTo(position());rider.finalizeSpawn(level,difficulty,reason,null,null);if(level.getLevel().addFreshEntity(rider)){rider.startRiding(this,true);spawnedRider=true;wasFreed=true;entityData.set(CAPTIVE,true);}}}return result;}
 @Override public void tick(){if(!level().isClientSide&&!WitcheryBridge.alchemistAvailable()){setNoAi(true);setTarget(null);}super.tick();if(!level().isClientSide){var rider=getPassengers().stream().filter(e->e instanceof Chort).map(e->(Chort)e).findFirst();entityData.set(CAPTIVE,rider.isPresent());rider.ifPresent(chort->{wasFreed=true;if(chort.getTarget()!=null)setTarget(chort.getTarget());});}}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){if(captive()||!WitcheryBridge.alchemistAvailable())return InteractionResult.sidedSuccess(level().isClientSide);if(!level().isClientSide&&isAlive()){String key=wasFreed?"tc.sloppy_alchemist.thanks"+(1+random.nextInt(3)):"tc.sloppy_alchemist.confused";player.displayClientMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.DARK_RED),false);playSound(ConciliumSounds.BREATH.get(),1,1);((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY()+.5,getZ(),20,.5,.5,.5,.02);if(wasFreed){int roll=random.nextInt(3);ItemStack reward=roll==1?WitcheryBridge.item("warm_blood",0):WitcheryBridge.item("infernal_blood",0);if(roll==2){var id=ResourceLocation.parse("bloodarsenal:blood_money");if(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id))reward=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id));}reward=reward.copy();reward.setCount(1+random.nextInt(3));if(!reward.isEmpty()&&!player.getInventory().add(reward)){var drop=spawnAtLocation(reward,.5f);if(drop!=null){drop.setPickUpDelay(40);drop.setDeltaMovement(drop.getDeltaMovement().add(0,.3,0));}}}discard();}return InteractionResult.sidedSuccess(level().isClientSide);}
 private final class Charge extends Goal {private LivingEntity victim;private int charge,windup,strafe,direction=1;private boolean winding,ramming,hit;private Vec3 destination=Vec3.ZERO;Charge(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){victim=getTarget();return captive()&&victim!=null&&victim.isAlive()&&WitcheryBridge.alchemistAvailable();}@Override public boolean canContinueToUse(){return captive()&&victim!=null&&victim.isAlive()&&WitcheryBridge.alchemistAvailable();}@Override public boolean requiresUpdateEveryTick(){return true;}@Override public void stop(){victim=null;winding=ramming=hit=false;charge=windup=0;getNavigation().stop();}
  @Override public void tick(){if(victim==null)return;Vec3 delta=victim.position().subtract(position());Vec3 toward=new Vec3(delta.x,0,delta.z).normalize();float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90);setYRot(yaw);yHeadRot=yBodyRot=yaw;
   if(winding){windup++;setDeltaMovement(0,windup==1&&onGround()?.45:getDeltaMovement().y,0);if(windup>=12){winding=false;ramming=true;hit=false;destination=victim.position().add(toward.scale(3));}return;}
   if(ramming){var offset=destination.subtract(position());var forward=new Vec3(offset.x,0,offset.z).normalize();setDeltaMovement(forward.x*1.1,getDeltaMovement().y,forward.z*1.1);if(!hit&&delta.horizontalDistanceSqr()<2.2&&delta.y*delta.y<2.2){doHurtTarget(victim);victim.push(toward.x*2.5,.65,toward.z*2.5);victim.hurtMarked=true;hit=true;}if(hit||offset.horizontalDistance()<.6||horizontalCollision){ramming=false;charge=0;}return;}
   if(delta.horizontalDistanceSqr()<256){if(strafe--<=0){strafe=30;direction=random.nextBoolean()?1:-1;}setDeltaMovement(-toward.x*.22+toward.z*.1*direction,getDeltaMovement().y,-toward.z*.22-toward.x*.1*direction);getNavigation().stop();}if(getSensing().hasLineOfSight(victim))charge++;else charge=0;if(charge>=60){winding=true;windup=charge=0;}
  }
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("WasFreed",wasFreed);tag.putBoolean("SpawnedRider",spawnedRider);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);wasFreed=tag.getBoolean("WasFreed");spawnedRider=tag.getBoolean("SpawnedRider");}
}
