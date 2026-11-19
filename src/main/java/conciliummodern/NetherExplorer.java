package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Recoverable floating remains, with server-authoritative buoyancy and collection. */
public final class NetherExplorer extends PathfinderMob {
    public NetherExplorer(EntityType<? extends NetherExplorer> type,Level level){super(type,level);setNoAi(true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,.5).add(Attributes.MOVEMENT_SPEED,0);}
    @Override public boolean canBreatheUnderwater(){return true;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType type){return true;}
    @Override public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level){return level.isUnobstructed(this);}
    @Override public void travel(Vec3 input){
        if(level().isClientSide)return;
        double submerged=0;var bounds=getBoundingBox();
        for(int slice=0;slice<5;slice++){
            double low=bounds.minY+bounds.getYsize()*slice/5-.125,high=bounds.minY+bounds.getYsize()*(slice+1)/5-.125;
            boolean wet=false;
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,low,bounds.minZ),BlockPos.containing(bounds.maxX,high,bounds.maxZ))){
                if(!level().hasChunkAt(pos))continue;var fluid=level().getFluidState(pos);
                if((fluid.is(FluidTags.WATER)||fluid.is(FluidTags.LAVA)) && pos.getY()+fluid.getHeight(level(),pos)>low){wet=true;break;}
            }
            if(wet)submerged+=.2;
        }
        var motion=getDeltaMovement();double vertical=motion.y;
        if(submerged<.999)vertical+=.04*(submerged*2-1);else {if(vertical<0)vertical/=2;vertical+=.007;}
        motion=new Vec3(motion.x,vertical,motion.z);double horizontal=motion.horizontalDistance();
        if(horizontal>.45)motion=new Vec3(motion.x*.45/horizontal,motion.y,motion.z*.45/horizontal);
        if(onGround())motion=motion.scale(.5);
        move(MoverType.SELF,motion);setDeltaMovement(motion.multiply(.99,.95,.99));
        if(motion.horizontalDistanceSqr()>.001){float wanted=(float)(Math.atan2(-motion.z,-motion.x)*180/Math.PI);setYRot(getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(wanted-getYRot()),-20,20));}
        setXRot(0);yBodyRot=getYRot();
    }
    private void unlock(Player player){
        if(player instanceof ServerPlayer server && !thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"thaumicconcilium:NETHER_MEMBRANE")){
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(server,"thaumicconcilium:NETHER_MEMBRANE");
            level().playSound(null,blockPosition(),tc4revival.content.TCSounds.HEARTBEAT.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }
    }
    private void dropMembrane(){var drop=spawnAtLocation(new ItemStack(ConciliumItems.NETHER_MEMBRANE.get()),.5f);if(drop!=null){drop.setDeltaMovement(drop.getDeltaMovement().add(0,.3,0));drop.setPickUpDelay(40);}}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!level().isClientSide && isAlive()){
            var stack=new ItemStack(ConciliumItems.NETHER_MEMBRANE.get());if(!player.getInventory().add(stack))dropMembrane();unlock(player);discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean hurt(DamageSource source,float damage){
        if(isRemoved() || isInvulnerableTo(source) || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || damage<=0)return false;
        if(!level().isClientSide){dropMembrane();if(source.getEntity() instanceof Player player)unlock(player);discard();}return true;
    }
}
