package conciliummodern;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

/** Shared signal/impulse projectile. Fireworks are visual and do not inherit rocket damage. */
public final class CompressedBlast extends Projectile {
    private static final EntityDataAccessor<ItemStack> FIREWORK=SynchedEntityData.defineId(CompressedBlast.class,EntityDataSerializers.ITEM_STACK);
    private int age,lifetime=20,power;
    private boolean vacuum,pneumatic,flashbang;
    public CompressedBlast(EntityType<? extends CompressedBlast> type,Level level) { super(type,level); }
    @Override protected void defineSynchedData() { entityData.define(FIREWORK,ItemStack.EMPTY); }
    public void initialize(Player player,ItemStack stack,ItemStack firework) {
        var wand=(CastingWandItem)stack.getItem();var focus=WandFocusItem.attached(stack);
        setOwner(thaumcraft.api.wands.WandActors.resolve(player));setPos(player.getX(),player.getEyeY(),player.getZ());
        setDeltaMovement(player.getLookAngle());entityData.set(FIREWORK,firework.copy());
        power=Math.max(0,(int)wand.focusPotency(stack,player));
        vacuum=SignalFocus.has(focus,"vacuum");pneumatic=SignalFocus.has(focus,"pneumo_strike");flashbang=SignalFocus.has(focus,"flashbang");
        lifetime=5*(firework.isEmpty()?1:3)+random.nextInt(20)+10*wand.focusUpgrade(stack,FocusUpgrade.EXTEND);
    }
    @Override public void tick() {
        super.tick();
        var next=position().add(getDeltaMovement());
        if(!level().isClientSide && (!level().hasChunkAt(net.minecraft.core.BlockPos.containing(next)) || level().isOutsideBuildHeight(net.minecraft.core.BlockPos.containing(next)))){discard();return;}
        move(MoverType.SELF,getDeltaMovement());updateRotation();age++;
        if(level().isClientSide) {
            level().addParticle(ParticleTypes.FIREWORK,getX(),getY(),getZ(),random.nextGaussian()*.05,-getDeltaMovement().y*.5,random.nextGaussian()*.05);
            if(age%5==0)level().addParticle(ParticleTypes.FLAME,getX(),getY(),getZ(),0,0,0);
            return;
        }
        if(vacuum)for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(4))) {
            if(target instanceof Player)continue;
            target.setDeltaMovement(position().subtract(target.position()).normalize().scale(2));target.hurtMarked=true;
        }
        if(age%5==0)playSound(ConciliumSounds.THUMP.get(),.9f,2.6f+(random.nextFloat()-random.nextFloat())*.8f);
        if(age>lifetime) {
            level().broadcastEntityEvent(this,(byte)17);
            if(entityData.get(FIREWORK).isEmpty()) {
                if(pneumatic)for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(10))) {
                    var direction=new net.minecraft.world.phys.Vec3(target.getX()-getX(),0,target.getZ()-getZ()).normalize();
                    target.setDeltaMovement(direction.x*.5*power,.5*power,direction.z*.5*power);target.hurtMarked=true;
                }
                else {
                    // Attribution and Forge explosion hooks retain modern protection interoperability.
                    boolean grief=net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level(),this);
                    level().explode(this,getX(),getY(),getZ(),1+power,grief?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.NONE);
                }
            } else if(flashbang)for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(10)))
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,3));
            discard();
        }
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==17 && level().isClientSide) {
            var firework=entityData.get(FIREWORK);var motion=getDeltaMovement();
            if(!firework.isEmpty())level().createFireworks(getX(),getY(),getZ(),motion.x,motion.y,motion.z,firework.getOrCreateTag().getCompound("Fireworks"));
            else level().addParticle(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),0,0,0);
        } else super.handleEntityEvent(event);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("Life",age);tag.putInt("LifeTime",lifetime);tag.putInt("power",power);
        tag.putBoolean("vacuum",vacuum);tag.putBoolean("pneumoStrike",pneumatic);tag.putBoolean("flashbang",flashbang);
        tag.put("FireworksItem",entityData.get(FIREWORK).save(new CompoundTag()));
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);age=Math.max(0,tag.getInt("Life"));lifetime=Math.max(1,tag.getInt("LifeTime"));power=Math.max(0,tag.getInt("power"));
        vacuum=tag.getBoolean("vacuum");pneumatic=tag.getBoolean("pneumoStrike");flashbang=tag.getBoolean("flashbang");
        entityData.set(FIREWORK,ItemStack.of(tag.getCompound("FireworksItem")));
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
