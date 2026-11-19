package tbmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class NodeManipulatorBlock extends BaseEntityBlock {
    public NodeManipulatorBlock() { super(Properties.of().strength(1).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NodeManipulator(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.MANIPULATOR.get(), NodeManipulator::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof NodeManipulator device)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        String mode = Content.NODE_MODES.stream().filter(id -> held.is(Content.item("node_focus_" + id))).findFirst().orElse("");
        if (!held.isEmpty() && mode.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!device.mode().isEmpty()) popResource(level, pos, new ItemStack(Content.item("node_focus_" + device.mode())));
            device.setMode(mode);
            if (!mode.isEmpty()) held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof NodeManipulator device && !device.mode().isEmpty())
            popResource(level, pos, new ItemStack(Content.item("node_focus_" + device.mode())));
        super.onRemove(state, level, pos, replacement, moving);
    }
}
