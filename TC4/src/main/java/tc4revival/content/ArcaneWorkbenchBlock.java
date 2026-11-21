package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public  class ArcaneWorkbenchBlock extends BaseEntityBlock {
    public ArcaneWorkbenchBlock() { super(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneWorkbenchBlockEntity(pos, state); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity table) {
            tc4revival.research.KnowledgeSync.flushBeforeScreen(server);
            net.minecraftforge.network.NetworkHooks.openScreen(server, table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity table) {
            if (!level.isClientSide) Containers.dropContents(level, pos, table);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    public static InteractionResult form(net.minecraft.world.item.ItemStack wand, net.minecraft.world.item.context.UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos(); var state = level.getBlockState(pos);
        if (!state.is(TCTables.TABLE.get()) || state.getValue(TableBlock.PART) != TableBlock.Part.SINGLE) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (!level.setBlock(pos, TCArcane.BLOCK.get().defaultBlockState(), 3)) return InteractionResult.FAIL;
        if (level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity table && !((CastingWandItem)wand.getItem()).isStaff(wand)) {
            var inserted = wand.copy(); inserted.setCount(1); table.setItem(10, inserted); wand.shrink(1);
        }
        level.playSound(null, pos.getX() + .5, pos.getY() + .1, pos.getZ() + .5,
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), net.minecraft.sounds.SoundSource.BLOCKS, .15f, .5f);
        return InteractionResult.CONSUME;
    }
}
