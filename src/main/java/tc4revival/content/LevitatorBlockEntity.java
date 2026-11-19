package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public  class LevitatorBlockEntity extends BlockEntity {
    private int counter;
    public int rangeAbove;
    public boolean requiresUpdate=true,lastPowerState;
    public LevitatorBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.LEVITATOR_ENTITY.get(),pos,state); }
    public boolean powered() { return level!=null && (level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above())); }
    public static void tick(Level level,BlockPos pos,BlockState state,LevitatorBlockEntity entity) {
        if (++entity.counter%100==0 || entity.requiresUpdate) {
            entity.lastPowerState=entity.powered(); entity.requiresUpdate=false;
            int max=10;
            for (var below=pos.below(); below.getY()>=level.getMinBuildHeight() && level.getBlockState(below).is(TCArcane.LEVITATOR.get()) && !level.hasNeighborSignal(below); below=below.below()) max+=10;
            entity.rangeAbove=0;
            while (entity.rangeAbove<max && pos.getY()+1+entity.rangeAbove<level.getMaxBuildHeight()) {
                var above=pos.above(1+entity.rangeAbove);
                if (level.getBlockState(above).isSolidRender(level,above)) break;
                entity.rangeAbove++;
            }
        }
        if (entity.rangeAbove<=0 || entity.powered()) return;
        boolean shift=level.isClientSide && tc4revival.client.LevitatorEffects.shiftDown();
        var column=new AABB(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+1,pos.getY()+1+entity.rangeAbove,pos.getZ()+1);
        for (var target:level.getEntities((Entity)null,column)) {
            if (!(target instanceof ItemEntity) && !target.isPushable() && !(target instanceof AbstractHorse)) continue;
            var velocity=target.getDeltaMovement(); double y=velocity.y;
            if (shift) { if (y<0) y*=(double).9f; }
            else if (y<(double).35f) y+=(double).1f;
            target.setDeltaMovement(velocity.x,y,velocity.z); target.fallDistance=0;
        }
    }
}
