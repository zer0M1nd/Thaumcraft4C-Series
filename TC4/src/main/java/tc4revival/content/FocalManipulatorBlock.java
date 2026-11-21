package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public  class FocalManipulatorBlock extends BaseEntityBlock {
    public FocalManipulatorBlock() { super(Properties.of().strength(3, 15).requiresCorrectToolForDrops().noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FocalManipulatorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCArcane.FOCAL_ENTITY.get(), (world, pos, block, table) -> table.tick());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table) {
            if (!tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(server.getUUID())
                    .map(knowledge -> knowledge.researchMarkers().contains("FOCALMANIPULATION")).orElse(false)) {
                server.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.researchmissing")
                        .withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            tc4revival.research.KnowledgeSync.flushBeforeScreen(server);
            net.minecraftforge.network.NetworkHooks.openScreen(server, table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table)
            net.minecraft.world.Containers.dropContents(level, pos, table);
        super.onRemove(state, level, pos, next, moving);
    }
}
