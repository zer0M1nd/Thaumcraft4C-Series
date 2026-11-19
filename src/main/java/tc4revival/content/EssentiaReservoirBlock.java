package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public  class EssentiaReservoirBlock extends BaseEntityBlock {
    public EssentiaReservoirBlock() { super(Properties.of().strength(2,17).sound(SoundType.METAL).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EssentiaReservoirBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==TCArcane.ESSENTIA_RESERVOIR_ENTITY.get() ? (world,pos,block,entity)->((EssentiaReservoirBlockEntity)entity).tick() : null;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        int amount=level.getBlockEntity(pos) instanceof EssentiaReservoirBlockEntity reservoir ? reservoir.amount() : 0;
        return amount*14/256+(amount>0 ? 1 : 0);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if (state.is(replacement.getBlock())) return;
        int spill=level.getBlockEntity(pos) instanceof EssentiaReservoirBlockEntity reservoir ? reservoir.amount()/16 : 0;
        // Remove the entity before the explosion, so its callbacks cannot spill this reservoir twice.
        super.onRemove(state,level,pos,replacement,moving);
        if (level.isClientSide || spill==0) return;
        level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,false,Level.ExplosionInteraction.NONE);
        int placed=0;
        for (int attempt=0;attempt<50;attempt++) {
            var target=pos.offset(level.random.nextInt(5)-level.random.nextInt(5),level.random.nextInt(5)-level.random.nextInt(5),level.random.nextInt(5)-level.random.nextInt(5));
            if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target) || !level.isEmptyBlock(target)) continue;
            var fluid=target.getY()<pos.getY() ? TCArcane.FLUX_GOO.get() : TCArcane.FLUX_GAS.get();
            level.setBlock(target,fluid.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,1),3);
            if (placed++>=spill) break;
        }
    }
}
