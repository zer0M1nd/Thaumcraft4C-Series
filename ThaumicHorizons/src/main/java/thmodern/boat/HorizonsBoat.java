package thmodern.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import thmodern.Horizons;

/** Native boat seating, paddles, input packets, interpolation, collisions and save format. */
public final class HorizonsBoat extends Boat {
    private final boolean metal;
    private boolean forward, reverse;
    public HorizonsBoat(EntityType<? extends HorizonsBoat> type, Level level, boolean metal) {
        super(type,level);this.metal=metal;
    }
    public boolean metal(){return metal;}
    public float damage(){return getDamage();}
    public int hurtTime(){return getHurtTime();}
    public int hurtDirection(){return getHurtDir();}
    public Item dropItem(){return getDropItem();}
    @Override public Item getDropItem(){return (metal?Horizons.THAUMIUM_BOAT_ITEM:Horizons.GREATWOOD_BOAT_ITEM).get();}
    @Override public boolean canBoatInFluid(FluidState state){return metal&&state.is(FluidTags.LAVA)||super.canBoatInFluid(state);}
    @Override public boolean canBoatInFluid(FluidType type){return metal&&type==ForgeMod.LAVA_TYPE.get()||super.canBoatInFluid(type);}
    @Override public boolean shouldUpdateFluidWhileRiding(FluidState state,Entity rider){
        return !(metal&&state.is(FluidTags.LAVA)&&!isUnderWater())&&super.shouldUpdateFluidWhileRiding(state,rider);
    }
    @Override protected void addPassenger(Entity rider){super.addPassenger(rider);protect(rider);}
    private void protect(Entity rider){
        if(metal&&!level().isClientSide&&rider instanceof LivingEntity living){
            living.clearFire();living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,10,0,true,false));
        }
    }
    @Override public void tick(){super.tick();getPassengers().forEach(this::protect);}
    @Override public void setInput(boolean left,boolean right,boolean forward,boolean reverse){
        super.setInput(left,right,forward,reverse);this.forward=forward;this.reverse=reverse;
    }
    @Override public void move(MoverType type,Vec3 motion){
        // Lava renders as an opaque surface, unlike vanilla water. A shallower draft keeps
        // the native hull floor above it, without changing boat seating or masking terrain.
        if(type==MoverType.SELF&&metal&&isControlledByLocalInstance()
                &&level().getFluidState(blockPosition()).is(FluidTags.LAVA)
                &&!level().getFluidState(blockPosition().above()).is(FluidTags.LAVA)){
            motion=motion.add(0,.02,0);setDeltaMovement(motion);
        }
        if(type==MoverType.SELF&&isControlledByLocalInstance()&&getControllingPassenger()!=null
                &&canBoatInFluid(level().getFluidState(blockPosition()))){
            var horizontal=BoatMotion.row(metal,motion.x,motion.z,getYRot(),forward&&!reverse);
            motion=new Vec3(horizontal.x(),motion.y,horizontal.z());setDeltaMovement(motion);
        }
        super.move(type,motion);
    }
    // Both original Horizons boats survive drops; bypass vanilla plank-and-stick destruction.
    @Override protected void checkFallDamage(double distance,boolean grounded,BlockState state,BlockPos pos){fallDistance=0;}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
}
