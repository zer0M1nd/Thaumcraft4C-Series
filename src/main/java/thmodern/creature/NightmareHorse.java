package thmodern.creature;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import thmodern.Horizons;

public final class NightmareHorse extends InfusedHorse {
    private int travelCooldown;
    public NightmareHorse(EntityType<? extends Horse> type,Level level){super(type,level);}
    @Override public boolean ender(){return true;}
    @Override public boolean fireImmune(){return true;}
    @Override public void handleStartJump(int power){
        if(power>=90&&(level().dimension()==Level.OVERWORLD||level().dimension()==Level.NETHER)){
            if(isSaddled()&&getControllingPassenger() instanceof ServerPlayer player)travel(player);
        }else super.handleStartJump(power);
    }
    public boolean travel(ServerPlayer player){
        if(!(level() instanceof ServerLevel source)||!isAlive()||travelCooldown>0||getPassengers().size()!=1||getControllingPassenger()!=player||!player.isAlive()||!isSaddled())return false;
        var key=source.dimension()==Level.OVERWORLD?Level.NETHER:source.dimension()==Level.NETHER?Level.OVERWORLD:null;if(key==null)return false;
        var destination=source.getServer().getLevel(key);if(destination==null||destination.getEntity(getUUID())!=null)return false;
        if(!net.minecraftforge.common.ForgeHooks.onTravelToDimension(this,key)||!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player,key))return false;
        double scale=net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(source.dimensionType(),destination.dimensionType());
        var spot=NightmareLanding.find(destination,getX()*scale,getY(),getZ()*scale);if(spot==null)return false;
        var data=new CompoundTag();if(!save(data))return false;data.remove("Passengers");data.remove("Leash");
        var replacement=Horizons.NIGHTMARE.get().create(destination);if(replacement==null)return false;replacement.load(data);replacement.moveTo(spot.x,spot.y,spot.z,getYRot(),getXRot());replacement.setDeltaMovement(Vec3.ZERO);replacement.travelCooldown=40;
        if(!destination.addFreshEntity(replacement))return false;
        var departure=position();player.stopRiding();player.teleportTo(destination,spot.x,spot.y+replacement.getPassengersRidingOffset(),spot.z,getYRot(),getXRot());
        if(player.level()!=destination){replacement.discard();player.startRiding(this,true);return false;}
        discard();player.startRiding(replacement,true);replacement.positionRider(player);player.fallDistance=0;
        // Depart only after the destination entity exists. A failed preparation must not explode.
        source.explode(null,departure.x,departure.y+getBbHeight()/2,departure.z,2,true,Level.ExplosionInteraction.MOB);return true;
    }
    @Override public void tick(){
        if(isInLava()){setDeltaMovement(getDeltaMovement().x,Math.min(.25,getDeltaMovement().y+.1),getDeltaMovement().z);setOnGround(true);fallDistance=0;}
        else if(level().getFluidState(blockPosition().below()).is(FluidTags.LAVA)){setOnGround(true);fallDistance=0;if(getDeltaMovement().y<0)setDeltaMovement(getDeltaMovement().multiply(1,0,1));}
        super.tick();if(level().isClientSide||!isAlive())return;if(travelCooldown>0)travelCooldown--;
        if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level(),this))return;
        var below=blockPosition().below();if(level().getBlockState(below).is(Blocks.GRASS_BLOCK))level().setBlock(below,Blocks.DIRT.defaultBlockState(),3);
        for(var pos:java.util.List.of(blockPosition(),blockPosition().above())){
            var state=level().getBlockState(pos);if(level().getBlockEntity(pos)==null&&(state.is(BlockTags.LEAVES)||state.is(Blocks.COBWEB)||state.is(Blocks.VINE)||state.getBlock() instanceof BushBlock)){
                level().removeBlock(pos,false);((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,15,.5,.5,.5,0);
            }
        }
    }
    @Override public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,Vec3 multiplier){if(!state.is(Blocks.COBWEB))super.makeStuckInBlock(state,multiplier);else fallDistance=0;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("TravelCooldown",travelCooldown);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);travelCooldown=Math.max(0,Math.min(40,tag.getInt("TravelCooldown")));}
}
