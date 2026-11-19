package thmodern.node;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class NodeDeviceBlock<T extends BlockEntity> extends BaseEntityBlock {
    private final Supplier<BlockEntityType<T>> type;
    private final java.util.function.Consumer<T> tick;
    public NodeDeviceBlock(Properties properties, Supplier<BlockEntityType<T>> type, java.util.function.Consumer<T> tick) {
        super(properties.noOcclusion()); this.type=type; this.tick=tick;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return type.get().create(pos,state); }
    @Override public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> actual) {
        return createTickerHelper(actual,type.get(),(l,p,s,e)->tick.accept(e));
    }
}
