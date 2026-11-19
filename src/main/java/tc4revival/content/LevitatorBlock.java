package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public  class LevitatorBlock extends BaseEntityBlock {
    public LevitatorBlock() { super(Properties.of().strength(2.5f,9).sound(SoundType.WOOD)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.or(box(0,0,0,16,16,.01),box(0,0,15.99,16,16,16),box(0,0,0,.01,16,16),box(15.99,0,0,16,16,16));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LevitatorBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return createTickerHelper(type,TCArcane.LEVITATOR_ENTITY.get(),LevitatorBlockEntity::tick); }
    @Override public boolean canConnectRedstone(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return side!=null && side.getAxis().isHorizontal(); }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) { if (!old.is(this)) invalidateBelow(level,pos); super.onPlace(state,level,pos,old,moving); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) { if (!next.is(this)) invalidateBelow(level,pos); super.onRemove(state,level,pos,next,moving); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos source,boolean moving) {
        if (level.getBlockEntity(pos) instanceof LevitatorBlockEntity entity && entity.powered()!=entity.lastPowerState) invalidateBelow(level,pos);
        super.neighborChanged(state,level,pos,block,source,moving);
    }
    private void invalidateBelow(Level level,BlockPos pos) {
        for (var below=pos.below(); below.getY()>=level.getMinBuildHeight() && level.getBlockState(below).is(this); below=below.below())
            if (level.getBlockEntity(below) instanceof LevitatorBlockEntity entity) entity.requiresUpdate=true;
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LevitatorBlockEntity entity && !entity.powered() && entity.rangeAbove>0)
            tc4revival.client.LevitatorEffects.sparkle(level,pos,random);
    }
}
