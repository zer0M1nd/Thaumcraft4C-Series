package thmodern.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A capturable wandering soul, not a villager or a second copy of a stored creature. */
public final class SoulEntity extends FlyingMob {
    private Vec3 waypoint=Vec3.ZERO;
    private int cooldown;
    public SoulEntity(EntityType<? extends SoulEntity> type,Level level){super(type,level);xpReward=5;setPersistenceRequired();setNoGravity(true);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,1);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return tc4revival.content.TCSounds.WISP_LIVE.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return tc4revival.content.TCSounds.WISP_DEAD.get();}
    @Override protected float getSoundVolume(){return .25f;}
    @Override protected void customServerAiStep(){
        var delta=waypoint.subtract(position());double length=delta.length();
        if(length<1||length>60){waypoint=position().add((random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16);delta=waypoint.subtract(position());length=delta.length();}
        if(--cooldown<=0){cooldown=2+random.nextInt(5);
            if(length>.0001&&traversable(delta,length))setDeltaMovement(getDeltaMovement().add(delta.scale(.1/length)));else waypoint=position();
        }
        setYRot((float)(-Math.atan2(getDeltaMovement().x,getDeltaMovement().z)*180/Math.PI));yBodyRot=getYRot();
    }
    private boolean traversable(Vec3 delta,double length){
        var step=delta.scale(1/length);var box=getBoundingBox();for(int i=1;i<length;i++){box=box.move(step);var pos=BlockPos.containing(box.getCenter());if(!level().hasChunkAt(pos)||!level().noCollision(this,box))return false;}
        var end=BlockPos.containing(waypoint);if(!level().hasChunkAt(end)||!level().getFluidState(end).isEmpty())return false;
        for(int i=0;i<11;i++)if(!level().isEmptyBlock(end.below(i)))return true;return false;
    }
    @Override public void tick(){super.tick();if(level().isClientSide&&random.nextBoolean())thaumcraft.api.fx.LegacyEffects.wisp(level(),position(),0x00ffff,.1f,1,.7f,.02f);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);}
}
