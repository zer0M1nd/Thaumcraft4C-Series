package ttmodern;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class RestorerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public RestorerBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5,10).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RestorerEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.RESTORER_ENTITY.get(),(world,pos,s,entity)->entity.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof RestorerEntity entity))return InteractionResult.PASS;
        if(!level.isClientSide) {
            if(!entity.inventory.getStackInSlot(0).isEmpty()) {
                var result=entity.removeItem();if(!player.getInventory().add(result))player.drop(result,false);
            }else if(RestorerEntity.repairable(player.getItemInHand(hand)))entity.insertItem(player.getItemInHand(hand).split(1));
            else return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!state.is(replacement.getBlock())&&level.getBlockEntity(pos) instanceof RestorerEntity entity)Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),entity.removeItem());
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof RestorerEntity entity && RestorerEntity.repairable(entity.inventory.getStackInSlot(0))?15:0;}
}
