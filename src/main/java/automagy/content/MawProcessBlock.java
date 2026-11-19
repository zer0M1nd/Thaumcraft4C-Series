package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class MawProcessBlock extends BaseEntityBlock {
    public MawProcessBlock(){super(Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MawProcessBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.MAW_PROCESS_TILE.get(),MawProcessBlockEntity::tick);}
}
