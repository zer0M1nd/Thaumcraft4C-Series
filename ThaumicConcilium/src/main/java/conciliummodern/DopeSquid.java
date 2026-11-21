package conciliummodern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
/** The original flying tobacco familiar, acquired through a Horizons vat infusion. */
public final class DopeSquid extends TamableAnimal {
 private static final EntityDataAccessor<String> TOBACCO=SynchedEntityData.defineId(DopeSquid.class,EntityDataSerializers.STRING);
 public float squidPitch,previousPitch,squidYaw,previousYaw,tentacleAngle,previousTentacle;private float rotation,velocity=.15f,motionSpeed,pulse;private int smokeClock;
 public DopeSquid(EntityType<? extends DopeSquid> type,Level level){super(type,level);setNoGravity(true);velocity=1/(random.nextFloat()+1)*.2f;}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,.1);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TOBACCO,"");}
 public ItemStack tobacco(){var id=ResourceLocation.tryParse(entityData.get(TOBACCO));return id==null||!ForgeRegistries.ITEMS.containsKey(id)?ItemStack.EMPTY:new ItemStack(ForgeRegistries.ITEMS.getValue(id));}
 private static boolean isTobacco(ItemStack stack){if(stack.isEmpty())return false;try{return Class.forName("tbmodern.api.Tobacco").isInstance(stack.getItem());}catch(ClassNotFoundException ignored){return false;}}
 @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){return null;}
 @Override public boolean isFood(ItemStack stack){return false;}
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public boolean removeWhenFarAway(double distance){return !isTame();}
 @Override public InteractionResult mobInteract(Player player,InteractionHand hand){var stack=player.getItemInHand(hand);boolean leaves=ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(ResourceLocation.parse("thaumicbases:tobacco_leaves"));if(!leaves&&!isOwnedBy(player))return super.mobInteract(player,hand);if(!level().isClientSide){
 if(isOwnedBy(player)){if(isTobacco(stack))entityData.set(TOBACCO,ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());else{setOrderedToSit(!isOrderedToSit());setInSittingPose(isOrderedToSit());}}
 if(leaves&&(!isTame()||isOwnedBy(player))){if(!isTame()){tame(player);setOrderedToSit(true);setInSittingPose(true);}else heal(5);level().broadcastEntityEvent(this,(byte)7);if(!player.getAbilities().instabuild)stack.shrink(1);}}
 return InteractionResult.sidedSuccess(level().isClientSide);}
 @Override public void travel(Vec3 ignored){move(MoverType.SELF,getDeltaMovement());}
 @Override public void aiStep(){super.aiStep();previousPitch=squidPitch;previousYaw=squidYaw;previousTentacle=tentacleAngle;rotation+=velocity;if(rotation>Math.PI*2){rotation-=Math.PI*2;if(random.nextInt(10)==0)velocity=1/(random.nextFloat()+1)*.2f;}
 if(!level().isClientSide)smokeClock=(smokeClock+1)%400;
 if(isOrderedToSit()){tentacleAngle=Math.abs((float)Math.sin(rotation))*(float)Math.PI*.25f;if(!level().isClientSide)setDeltaMovement(0,(getDeltaMovement().y-.08)*.98,0);squidPitch+=(-90-squidPitch)*.02f;return;}
 if(rotation<Math.PI){float fraction=rotation/(float)Math.PI;tentacleAngle=(float)Math.sin(fraction*fraction*Math.PI)*(float)Math.PI*.25f;if(fraction>.75){motionSpeed=.9f;pulse=1;}else pulse*=.8f;}else{tentacleAngle=0;motionSpeed*=.9f;pulse*=.99f;}
 if(!level().isClientSide){if(smokeClock==0){playSound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,1,1);getPersistentData().putInt("SmokeTicks",10);if(getOwner() instanceof ServerPlayer player&&player.level()==level()&&ConciliumFocus.warp(player,tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY)>=10){var dose=tobacco();if(isTobacco(dose)){try{Class.forName("tbmodern.api.Tobacco").getMethod("smoke",ServerPlayer.class,ItemStack.class,ItemStack.class,boolean.class).invoke(dose.getItem(),player,dose,ItemStack.EMPTY,false);}catch(ReflectiveOperationException ex){throw new IllegalStateException("Thaumic Bases tobacco API changed",ex);}}ConciliumFocus.changeWarp(player,tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY,-10);}}
 int smoke=getPersistentData().getInt("SmokeTicks");if(smoke>0){((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,getX(),getEyeY(),getZ(),1,.05,.05,.05,.05);getPersistentData().putInt("SmokeTicks",smoke-1);}
 var owner=getOwner();if(owner!=null&&owner.level()==level()){Vec3 toward=owner.position().add(random.nextInt(3)-1,random.nextInt(3)-1,random.nextInt(3)-1).subtract(position()).normalize();setDeltaMovement(getDeltaMovement().add(toward.scale(.1)).add(0,getY()<owner.getY()+2?.1:0,0).scale(motionSpeed));if(distanceToSqr(owner)>225)teleportNear(owner);}}
 var motion=getDeltaMovement();yBodyRot+=net.minecraft.util.Mth.wrapDegrees((float)-Math.toDegrees(Math.atan2(motion.x,motion.z))-yBodyRot)*.1f;setYRot(yBodyRot);squidYaw+=(float)Math.PI*pulse*1.5f;squidPitch+=((float)-Math.toDegrees(Math.atan2(motion.horizontalDistance(),motion.y))-squidPitch)*.1f;
 }
 private void teleportNear(LivingEntity owner){for(int i=0;i<12;i++){Vec3 dest=owner.position().add(random.nextInt(5)-2,random.nextInt(3)+1,random.nextInt(5)-2);var pos=net.minecraft.core.BlockPos.containing(dest);var bounds=getBoundingBox().move(dest.subtract(position()));if(level().hasChunkAt(pos)&&level().getWorldBorder().isWithinBounds(bounds)&&level().noCollision(this,bounds)){moveTo(dest);setDeltaMovement(Vec3.ZERO);break;}}}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){spawnAtLocation(new ItemStack(Items.INK_SAC,1+random.nextInt(3+Math.max(0,looting))));super.dropCustomDeathLoot(source,looting,hit);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putString("Tobacco",entityData.get(TOBACCO));tag.putInt("SmokeClock",smokeClock);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("Tobacco",8)){var id=ResourceLocation.tryParse(tag.getString("Tobacco"));if(id!=null&&ForgeRegistries.ITEMS.containsKey(id)&&isTobacco(new ItemStack(ForgeRegistries.ITEMS.getValue(id))))entityData.set(TOBACCO,id.toString());}smokeClock=net.minecraft.util.Mth.clamp(tag.getInt("SmokeClock"),0,399);setInSittingPose(isOrderedToSit());}
}
