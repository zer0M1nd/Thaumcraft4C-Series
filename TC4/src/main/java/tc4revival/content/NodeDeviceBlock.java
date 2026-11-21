package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public  class NodeDeviceBlock extends BaseEntityBlock {
    private final boolean converter;
    public NodeDeviceBlock(boolean converter) { super(Properties.of().strength(3, 15).requiresCorrectToolForDrops().noOcclusion()); this.converter = converter; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return converter ? new NodeConverterBlockEntity(pos, state) : new NodeStabilizerBlockEntity(pos, state); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (converter && level.getBlockEntity(pos) instanceof NodeConverterBlockEntity device) device.checkStatus();
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return converter ? createTickerHelper(type, TCArcane.NODE_CONVERTER_ENTITY.get(), (world, pos, block, device) -> device.tick())
                : createTickerHelper(type, TCArcane.NODE_STABILIZER_ENTITY.get(), (world, pos, block, device) -> device.tick());
    }
}
