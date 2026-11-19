package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
public final class Chort extends Monster {
 private int chatCooldown;
 public Chort(EntityType<? extends Chort> type,Level level){super(type,level);setCanPickUpLoot(false);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.MAX_HEALTH,25).add(Attributes.ATTACK_DAMAGE,8);}
 static boolean target(Monster mob,LivingEntity target){if(!(target instanceof Player player)||!WitcheryBridge.alchemistAvailable())return false;boolean pact=ImpPacts.remaining(player)>0;if(pact&&mob instanceof Chort chort&&chort.chatCooldown==0){player.displayClientMessage(Component.translatable("tc.chort.has_pact").withStyle(net.minecraft.ChatFormatting.DARK_RED),false);chort.chatCooldown=1200;}return !pact&&mob.getLastHurtByMob()!=player;}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new SpitAttack());goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,t->target(this,t)));}
 @Override public void tick(){if(!level().isClientSide&&!WitcheryBridge.alchemistAvailable()){setNoAi(true);setTarget(null);}super.tick();if(chatCooldown>0)chatCooldown--;if(!level().isClientSide&&getVehicle() instanceof SloppyAlchemist mount&&mount.getTarget()!=null)setTarget(mount.getTarget());}
 private final class SpitAttack extends Goal {private LivingEntity victim;private int cooldown;SpitAttack(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){victim=getTarget();return WitcheryBridge.alchemistAvailable()&&victim!=null&&victim.isAlive()&&distanceToSqr(victim)<=256;}@Override public boolean canContinueToUse(){return victim!=null&&victim.isAlive()&&WitcheryBridge.alchemistAvailable();}@Override public boolean requiresUpdateEveryTick(){return true;}@Override public void start(){cooldown=0;}@Override public void stop(){victim=null;}@Override public void tick(){if(victim==null)return;getLookControl().setLookAt(victim,30,30);if(cooldown>0){cooldown--;return;}for(int i=-1;i<=1;i++){var spit=RemainingMobs.CHORT_SPIT.get().create(level());if(spit!=null){spit.setOwner(Chort.this);spit.moveTo(getX(),getEyeY()-.1,getZ());var delta=victim.getEyePosition().subtract(getEyePosition());double angle=Math.toRadians(i*4);spit.shoot(delta.x*Math.cos(angle)-delta.z*Math.sin(angle),delta.y,delta.x*Math.sin(angle)+delta.z*Math.cos(angle),1.8f,1);level().addFreshEntity(spit);}}cooldown=40;}}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return net.minecraft.sounds.SoundEvents.PIG_AMBIENT;}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.PIG_AMBIENT;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.PIG_DEATH;}
 @Override public float getVoicePitch(){return .5f;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){if(WitcheryBridge.alchemistAvailable()){int roll=random.nextInt(10);ItemStack reward=roll<3?WitcheryBridge.item("foul_fume",0):roll<6?WitcheryBridge.item("exhale_of_the_horned_one",0):WitcheryBridge.brew("moonshine");reward=reward.copy();if(roll<6)reward.setCount(random.nextInt(4));if(!reward.isEmpty())spawnAtLocation(reward,1.5f);}super.dropCustomDeathLoot(source,looting,hit);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ChatCooldown",chatCooldown);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);chatCooldown=net.minecraft.util.Mth.clamp(tag.getInt("ChatCooldown"),0,1200);}
}
