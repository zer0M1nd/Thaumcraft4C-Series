package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Healing motes retain their caster across save/reload; caster pickup is allowed. */
public final class PositiveBurstOrb extends Projectile {
    private static final EntityDataAccessor<Integer> MODE=SynchedEntityData.defineId(PositiveBurstOrb.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(PositiveBurstOrb.class,EntityDataSerializers.INT);
    private int power,health=5;
    private Player target;
    public PositiveBurstOrb(EntityType<? extends PositiveBurstOrb> type,Level level) { super(type,level); }
    @Override protected void defineSynchedData() { entityData.define(MODE,0);entityData.define(AGE,0); }
    public int mode() { return entityData.get(MODE); }
    public int age() { return entityData.get(AGE); }
    public void initialize(Player player,ItemStack focus,int power,InteractionHand hand) {
        setOwner(thaumcraft.api.wands.WandActors.resolve(player));this.power=power;
        entityData.set(MODE,SignalFocus.has(focus,"fulfillment")?2:SignalFocus.has(focus,"vitaminize")?1:0);
        double yaw=Math.toRadians(player.getYRot());var look=player.getLookAngle();
        var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();double side=arm==HumanoidArm.RIGHT?1:-1;
        setPos(player.getX()+look.x-Math.cos(yaw)*.16*side,player.getEyeY()-.5,player.getZ()+look.z-Math.sin(yaw)*.16*side);
        setDeltaMovement(look.add(random.nextGaussian()*.09,random.nextGaussian()*.09,random.nextGaussian()*.09));
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && (!level().hasChunkAt(blockPosition()) || age()>=150)){discard();return;}
        var motion=getDeltaMovement().add(0,-.03,0);
        if(isInLava()) {
            motion=new Vec3((random.nextFloat()-random.nextFloat())*.2,.2,(random.nextFloat()-random.nextFloat())*.2);
            if(!level().isClientSide)playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE,.4f,2+random.nextFloat()*.4f);
        }
        if(!level().isClientSide) {
            if(target!=null && (!target.isAlive() || target.isSpectator() || target.level()!=level() || distanceToSqr(target)>64))target=null;
            if(tickCount%5==0 && target==null)target=level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(8),p->p!=getOwner() && p.isAlive() && !p.isSpectator())
                    .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if(target!=null) {
                var offset=target.getEyePosition().subtract(position()).scale(1/8.0);double length=offset.length();
                if(length>1.0e-6 && length<1)motion=motion.add(offset.scale((1-length)*(1-length)*.1/length));
            }
        }
        setDeltaMovement(motion);move(MoverType.SELF,motion);
        float drag=.98f;
        if(onGround()) {var pos=getBlockPosBelowThatAffectsMyMovement();drag=level().getBlockState(pos).getFriction(level(),pos,this)*.98f;}
        setDeltaMovement(motion.multiply(drag,onGround()?-.882:.98,drag));
        if(!level().isClientSide) {
            entityData.set(AGE,age()+1);
            for(var player:level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(.1))) {playerTouch(player);if(isRemoved())break;}
        }
    }
    @Override public void playerTouch(Player player) {
        if(level().isClientSide || isRemoved() || !player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable || player.takeXpDelay>0)return;
        if(mode()==2)player.setAbsorptionAmount(Math.min(20,player.getAbsorptionAmount()+power));
        else if(mode()==1) {
            var effect=switch(random.nextInt(3)) {case 0->MobEffects.MOVEMENT_SPEED;case 1->MobEffects.DAMAGE_RESISTANCE;default->MobEffects.DAMAGE_BOOST;};
            player.addEffect(new MobEffectInstance(effect,100+40*power,power));
        } else player.heal(.5f);
        player.takeXpDelay=2;playSound(SoundEvents.EXPERIENCE_ORB_PICKUP,.1f,.5f*((random.nextFloat()-random.nextFloat())*.7f+1.8f));discard();
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isInvulnerableTo(source) || level().isClientSide)return false;
        health=(int)(health-amount);if(health<=0)discard();return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("Health",health);tag.putInt("Age",age());tag.putInt("Power",power);
        tag.putBoolean("Fulfillment",mode()==2);tag.putBoolean("Vitaminize",mode()==1);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);health=tag.getInt("Health");entityData.set(AGE,Math.max(0,tag.getInt("Age")));power=Math.max(0,tag.getInt("Power"));
        entityData.set(MODE,tag.getBoolean("Fulfillment")?2:tag.getBoolean("Vitaminize")?1:0);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
