package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public  class DeconstructionTableBlock extends BaseEntityBlock {
    public DeconstructionTableBlock() { super(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new DeconstructionTableBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type,TCArcane.DECONSTRUCTION_TABLE_ENTITY.get(),DeconstructionTableBlockEntity::tick); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DeconstructionTableBlockEntity table) {
            tc4revival.research.KnowledgeSync.flushBeforeScreen(server);
            net.minecraftforge.network.NetworkHooks.openScreen(server,table,pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {
        if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof DeconstructionTableBlockEntity table) table.setCustomName(stack.getHoverName().getString());
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!next.is(this) && level.getBlockEntity(pos) instanceof DeconstructionTableBlockEntity table) {
            if (!level.isClientSide) Containers.dropContents(level,pos,table);
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
