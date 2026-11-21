package thmodern.creature;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import thmodern.Horizons;

public class InfusedHorse extends Horse {
    public InfusedHorse(EntityType<? extends Horse> type,Level level){super(type,level);refreshStats();}
    public boolean ender(){return getType()==Horizons.ENDERSTEED.get();}
    private void modifier(String key,Attribute attribute,double value,AttributeModifier.Operation operation){
        var instance=getAttribute(attribute);if(instance==null)return;var id=UUID.nameUUIDFromBytes(("thaumichorizons:horse_"+key).getBytes(StandardCharsets.UTF_8));
        if(instance.getModifier(id)==null)instance.addPermanentModifier(new AttributeModifier(id,key,value,operation));
    }
    public void refreshStats(){
        if(ender()){
            modifier("ender_speed",Attributes.MOVEMENT_SPEED,.1,AttributeModifier.Operation.MULTIPLY_BASE);
            modifier("ender_jump",Attributes.JUMP_STRENGTH,.25,AttributeModifier.Operation.MULTIPLY_BASE);
            modifier("ender_health",Attributes.MAX_HEALTH,4,AttributeModifier.Operation.MULTIPLY_BASE);
        }else if(getType()==Horizons.UNDEAD_HORSE.get())modifier("undead_health",Attributes.MAX_HEALTH,20,AttributeModifier.Operation.ADDITION);
        else if(getType()==Horizons.SKELETAL_HORSE.get())modifier("skeleton_speed",Attributes.MOVEMENT_SPEED,.1,AttributeModifier.Operation.ADDITION);
    }
    @Override public MobType getMobType(){return ender()?super.getMobType():MobType.UNDEAD;}
    @Override public boolean canMate(Animal partner){return ender()&&super.canMate(partner);}
    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack){return ender()&&super.isFood(stack);}
    @Override protected boolean handleEating(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack){return ender()&&super.handleEating(player,stack);}
    @Override public boolean canWearArmor(){return ender();}
    @Override public boolean isArmor(net.minecraft.world.item.ItemStack stack){return ender()&&super.isArmor(stack);}
    @Override public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(!ender()&&!isTamed())return net.minecraft.world.InteractionResult.PASS;
        return super.mobInteract(player,hand);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return ender()?super.getAmbientSound():getType()==Horizons.UNDEAD_HORSE.get()?net.minecraft.sounds.SoundEvents.ZOMBIE_HORSE_AMBIENT:net.minecraft.sounds.SoundEvents.SKELETON_HORSE_AMBIENT;}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return ender()?super.getDeathSound():getType()==Horizons.UNDEAD_HORSE.get()?net.minecraft.sounds.SoundEvents.ZOMBIE_HORSE_DEATH:net.minecraft.sounds.SoundEvents.SKELETON_HORSE_DEATH;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return ender()?super.getHurtSound(source):getType()==Horizons.UNDEAD_HORSE.get()?net.minecraft.sounds.SoundEvents.ZOMBIE_HORSE_HURT:net.minecraft.sounds.SoundEvents.SKELETON_HORSE_HURT;}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);refreshStats();}
    @Override public void onPlayerJump(int power){if(!ender())super.onPlayerJump(power);}
    @Override public void handleStartJump(int power){
        if(!ender()){super.handleStartJump(power);return;}
        if(isSaddled()&&getControllingPassenger() instanceof net.minecraft.server.level.ServerPlayer)blink(power);
    }
    public boolean blink(int power){
        if(!(level() instanceof ServerLevel level)||!ender()||!isAlive())return false;
        double distance=Math.max(0,Math.min(100,power))/7d,radians=Math.toRadians(getYRot());
        var event=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(this,getX()-distance*Math.sin(radians),getY(),getZ()+distance*Math.cos(radians));
        if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return false;
        if(!Double.isFinite(event.getTargetX())||!Double.isFinite(event.getTargetY())||!Double.isFinite(event.getTargetZ()))return false;
        var original=position();
        for(int y=(int)Math.floor(event.getTargetY())-3;y<=Math.floor(event.getTargetY())+4;y++){
            var pos=BlockPos.containing(event.getTargetX(),y,event.getTargetZ());if(y<=level.getMinBuildHeight()||y>=level.getMaxBuildHeight()-3||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))continue;
            if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
            var destination=new net.minecraft.world.phys.Vec3(event.getTargetX(),y,event.getTargetZ());var delta=destination.subtract(original);var box=getBoundingBox().move(delta);
            if(!level.noCollision(this,box)||level.containsAnyLiquid(box))continue;
            boolean riderSafe=true;for(var rider:getPassengers()){var bounds=rider.getBoundingBox().move(delta);if(!level.noCollision(rider,bounds)||level.containsAnyLiquid(bounds)){riderSafe=false;break;}}
            if(!riderSafe)continue;
            teleportTo(destination.x,destination.y,destination.z);for(var rider:getPassengers())positionRider(rider);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,original.x,original.y+1,original.z,64,.5,1,.5,.2);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,getX(),getY()+1,getZ(),64,.5,1,.5,.2);playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,1,1);fallDistance=0;return true;
        }return false;
    }
}
