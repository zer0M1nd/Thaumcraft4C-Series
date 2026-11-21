package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public  class CultistClericEntity extends CultistEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(CultistClericEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<BlockPos> HOME=SynchedEntityData.defineId(CultistClericEntity.class,EntityDataSerializers.BLOCK_POS);
    public CultistClericEntity(EntityType<? extends CultistClericEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return cultistAttributes().add(Attributes.MAX_HEALTH,30); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(FLAGS,(byte)0); entityData.define(HOME,BlockPos.ZERO); }
    public boolean ritualist() { return (entityData.get(FLAGS)&1)!=0; }
    public BlockPos ritualHome() { return entityData.get(HOME); }
    public void setRitualist(boolean value) { entityData.set(FLAGS,(byte)(value ? entityData.get(FLAGS)|1 : entityData.get(FLAGS)&~1)); }
    @Override public void restrictTo(BlockPos pos,int radius) { super.restrictTo(pos,radius); entityData.set(HOME,pos.immutable()); }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1,new Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP)); }
            @Override public boolean canUse() { return ritualist(); }
            @Override public boolean canContinueToUse() { return ritualist(); }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                var home=getRestrictCenter();
                if (tickCount%40==0 && (home.distSqr(new BlockPos((int)getX(),(int)getY(),(int)getZ()))>16 || !TCEldritch.isDevice(level().getBlockState(home).getBlock()))) setRitualist(false);
            }
        });
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24) {
            @Override public boolean canUse() { return super.canUse() && getTarget()!=null && getTarget().isAlive() && distanceToSqr(getTarget())>=4; }
        });
    }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,DifficultyInstance difficulty) {
        for (var armor:new CultistEquipment.Armor[]{CultistEquipment.Armor.ROBE_HELMET,CultistEquipment.Armor.ROBE_CHEST,CultistEquipment.Armor.ROBE_LEGS}) setItemSlot(armor.slot.getSlot(),new ItemStack(TCEldritch.armor(armor)));
        if (random.nextFloat()<(level().getDifficulty()==Difficulty.HARD ? .3f : .1f)) setItemSlot(EquipmentSlot.FEET,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.BOOTS)));
    }
    @Override public boolean removeWhenFarAway(double distance) { return !ritualist(); }
    @Override public boolean hurt(DamageSource source,float amount) { if (isInvulnerableTo(source)) return false; setRitualist(false); return super.hurt(source,amount); }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if (level().isClientSide) return; swing(InteractionHand.MAIN_HAND);
        double dx=target.getX()-getX(),dy=target.getY()+target.getBbHeight()/2-(getY()+getBbHeight()/2),dz=target.getZ()-getZ();
        if (random.nextFloat()>.66f) {
            var shot=new GolemOrbEntity(level(),this,target,true); shot.advanceCultistLaunch(); shot.shoot(dx,dy+2,dz,.66f,3); level().addFreshEntity(shot);
            playSound(TCSounds.ELDRITCH_ATTACK.get(),1,1+random.nextFloat()*.1f);
        } else {
            float spread=Mth.sqrt(power)*.5f; level().levelEvent(null,1018,blockPosition(),0);
            for (int count=0;count<3;count++) {
                var ball=new SmallFireball(level(),this,dx+random.nextGaussian()*spread,dy,dz+random.nextGaussian()*spread);
                ball.setPos(getX(),getY()+getBbHeight()/2+.5,getZ()); level().addFreshEntity(ball);
            }
        }
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide && ritualist()) {
            var home=entityData.get(HOME); double dx=home.getX()+.5-getX(),dy=home.getY()+1.5-getEyeY(),dz=home.getZ()+.5-getZ();
            float yaw=(float)Math.toDegrees(Math.atan2(dz,dx))-90,pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
            setXRot(getXRot()+Mth.clamp(Mth.wrapDegrees(pitch-getXRot()),-10,10));
            yHeadRot+=Mth.clamp(Mth.wrapDegrees(yaw-yHeadRot),-getMaxHeadXRot(),getMaxHeadXRot());
        }
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return TCSounds.CHANT.get(); }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("Flags",entityData.get(FLAGS)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(FLAGS,tag.getByte("Flags")); }
}
