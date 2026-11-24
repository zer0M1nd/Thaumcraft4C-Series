package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class StorageChestBlock extends ChestBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private final int capacity;

    public StorageChestBlock(int capacity) {
        super(Properties.copy(Blocks.CHEST), () -> TCArcane.STORAGE_CHEST_ENTITY.get());
        this.capacity = capacity;
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    public int capacity() { return capacity; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageChestBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TCArcane.STORAGE_CHEST_ENTITY.get(), StorageChestBlockEntity::tick);
    }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return combine(state, level, pos, false).apply(new DoubleBlockCombiner.Combiner<ChestBlockEntity, MenuProvider>() {
            public MenuProvider acceptSingle(ChestBlockEntity chest) { return chest; }
            public MenuProvider acceptNone() { return null; }
            public MenuProvider acceptDouble(ChestBlockEntity first, ChestBlockEntity second) {
                Component title = first.hasCustomName() ? first.getDisplayName() : second.hasCustomName()
                        ? second.getDisplayName() : Component.translatable(getDescriptionId() + ".double");
                return new SimpleMenuProvider((id, inventory, player) -> {
                    if (!first.canOpen(player) || !second.canOpen(player)) return null;
                    first.unpackLootTable(player);
                    second.unpackLootTable(player);
                    return new StorageChestMenu(id, inventory, new CompoundContainer(first, second));
                }, title);
            }
        });
    }
}
