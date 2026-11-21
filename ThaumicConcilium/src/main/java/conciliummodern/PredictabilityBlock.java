package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class PredictabilityBlock extends BaseEntityBlock {
    public PredictabilityBlock(){super(Properties.of().strength(.7f,100).sound(SoundType.STONE).lightLevel(state->7).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PredictabilityBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,ConciliumBlocks.HEX_TILE.get(),PredictabilityBlockEntity::tick);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PredictabilityBlockEntity tile)
            PredictabilityStructure.restore(level,tile.center(),pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var tile=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return tile instanceof PredictabilityBlockEntity hex && !hex.isMaster()?java.util.List.of(new net.minecraft.world.item.ItemStack(tc4revival.content.TCEldritch.STONE.get())):java.util.List.of();
    }
}
