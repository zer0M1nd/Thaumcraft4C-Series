package thaumicexploration;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.network.chat.Component;

public class BoundBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public final boolean jar;
    public BoundBlock(boolean jar) { super(Properties.copy(jar?Blocks.GLASS:Blocks.CHEST).strength(2.5F).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)); this.jar=jar;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override public RenderShape getRenderShape(BlockState state) { return jar?RenderShape.MODEL:RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(jar?3:1,0,jar?3:1,jar?13:15,jar?12:14,jar?13:15); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new BoundEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return createTickerHelper(type,ExplorationMod.BOUND.get(),(l,p,s,be)->be.tick()); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof SealItem) return InteractionResult.PASS;
        if (!jar && level.getBlockEntity(pos) instanceof BoundEntity be) {
            if (!level.isClientSide && !level.getBlockState(pos.above()).isRedstoneConductor(level,pos.above()))
                player.openMenu(new SimpleMenuProvider((id,inv,p)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv,be.chest()),Component.translatable("block.thaumicexploration.bound_chest")));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BoundEntity be) {
            if (!jar) { Containers.dropContents(level,pos,be.chest()); be.chest().clearContent(); }
            popResource(level,pos,new ItemStack(ExplorationMod.item("blank_seal_"+DyeColor.byId(be.color).getName())));
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
