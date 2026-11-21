package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.content.*;

/** Concentrated bottle: original taint burst plus flux gas over converted ground. */
public final class ThickTaintProjectile extends TaintBottleEntity {
    private boolean impacted;
    public ThickTaintProjectile(EntityType<? extends ThickTaintProjectile> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return ConciliumItems.THICK_TAINT.get(); }
    private boolean mayChange(ServerLevel level,BlockPos pos) {
        if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))return false;
        if(getOwner() instanceof ServerPlayer player) {
            if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,getItem()))return false;
        } else if(!ForgeEventFactory.getMobGriefingEvent(level,getOwner()==null?this:getOwner()))return false;
        return !ForgeEventFactory.onBlockPlace(getOwner()==null?this:getOwner(),BlockSnapshot.create(level.dimension(),level,pos),Direction.UP);
    }
    @Override protected void onHit(HitResult hit) {
        if(impacted || isRemoved() || !(level() instanceof ServerLevel level))return;
        impacted=true;
        var point=hit.getLocation();
        for(var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(5))) {
            if(!(living instanceof TaintedMob) && !living.isInvertedHealAndHarm())
                living.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),100,0,false,true));
        }
        var origin=BlockPos.containing(point);
        for(int attempt=0;attempt<10;attempt++) {
            var pos=origin.offset((int)((random.nextFloat()-random.nextFloat())*5),0,(int)((random.nextFloat()-random.nextFloat())*5));
            if(!random.nextBoolean() || !mayChange(level,pos) || level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(TCBiomes.TAINTED_LAND))continue;
            if(!WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND))continue;
            if(level.getBlockState(pos.below()).isRedstoneConductor(level,pos.below()) && level.getBlockState(pos).canBeReplaced())
                level.setBlock(pos,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,pos),3);
            var gas=pos.above();
            if(level.isEmptyBlock(gas) && mayChange(level,gas))level.setBlock(gas,TCArcane.FLUX_GAS.get().defaultBlockState(),3);
        }
        level.broadcastEntityEvent(this,(byte)3);
        discard();
    }
}
