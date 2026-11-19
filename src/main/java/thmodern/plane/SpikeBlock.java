package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class SpikeBlock extends BaseEntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.FACING;
 private final String kind;
 public SpikeBlock(String kind){super(Properties.of().strength(3,3).noOcclusion());this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
 public String kind(){return kind;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace());return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
 @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var facing=state.getValue(FACING);var support=pos.relative(facing.getOpposite());return level.getBlockState(support).isFaceSturdy(level,support,facing);}
 @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return side==state.getValue(FACING).getOpposite()&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,side,neighbor,level,pos,neighborPos);}
 @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){case UP->box(0,0,0,16,15,16);case DOWN->box(0,1,0,16,16,16);case NORTH->box(0,0,1,16,16,16);case SOUTH->box(0,0,0,16,16,15);case WEST->box(1,0,0,16,16,16);case EAST->box(0,0,0,15,16,16);};}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(!level.isClientSide)entity.hurt(level.damageSources().cactus(),1);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PlanarDecorations.SpikeEntity(pos,state);}
 @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
 @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
}
