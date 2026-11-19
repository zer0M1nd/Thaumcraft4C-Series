package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class LithographerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public LithographerBlock(){super(Properties.of().strength(3,17).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.X).setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AXIS,POWERED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(AXIS)==Direction.Axis.X?box(-6.4,-16,0,22.4,16,16):box(0,-16,-6.4,16,16,22.4);}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(AXIS)==Direction.Axis.X?Shapes.or(box(-6.4,-16,0,0,16,16),box(16,-16,0,22.4,16,16),box(0,12,0,16,16,16)):
                Shapes.or(box(0,-16,-6.4,16,16,0),box(0,-16,16,16,16,22.4),box(0,12,0,16,16,16));
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(AXIS,context.getHorizontalDirection().getClockWise().getAxis());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new LithographerBlockEntity(pos,state);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){if(!level.isClientSide && !old.is(this))level.scheduleTick(pos,this,1);}
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){neighborChanged(state,level,pos,this,pos,false);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,ConciliumBlocks.LITHOGRAPHER_TILE.get(),LithographerBlockEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof LithographerBlockEntity tile)
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new LithographerMenu(id,inventory,tile),net.minecraft.network.chat.Component.translatable("block.thaumicconcilium.lithographer")),pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(level.isClientSide)return;boolean powered=level.hasNeighborSignal(pos);
        if(powered!=state.getValue(POWERED)){level.setBlock(pos,state.setValue(POWERED,powered),2);if(powered && level.getBlockEntity(pos) instanceof LithographerBlockEntity tile)tile.project();}
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof LithographerBlockEntity tile){Containers.dropContents(level,pos,tile);level.updateNeighbourForOutputSignal(pos,this);}
        super.onRemove(state,level,pos,next,moving);
    }
}
