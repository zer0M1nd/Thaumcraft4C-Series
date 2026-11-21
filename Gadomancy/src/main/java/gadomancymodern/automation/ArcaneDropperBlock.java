package gadomancymodern.automation;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public class ArcaneDropperBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty FLIPPED=BooleanProperty.create("flipped");
    public ArcaneDropperBlock(){super(Properties.of().strength(3.5f).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(FLIPPED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,FLIPPED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction side=context.getClickedFace();float yaw=net.minecraft.util.Mth.wrapDegrees(context.getRotation()),pitch=context.getPlayer()==null?0:context.getPlayer().getXRot();boolean flipped;
        if(side.getAxis().isVertical()){yaw+=180;flipped=yaw>315||yaw<45||yaw<225&&yaw>135;}
        else {switch(side.getOpposite()){case WEST->yaw-=90;case NORTH->yaw=(180-Math.abs(yaw))*(yaw<0?1:-1);case EAST->yaw+=90;default->{}}flipped=Math.abs(yaw)<Math.abs(pitch);}
        return defaultBlockState().setValue(FACING,side).setValue(FLIPPED,flipped);
    }
    public static boolean automationFace(BlockState state,Direction side){
        Direction facing=state.getValue(FACING);boolean flip=state.getValue(FLIPPED);
        if(facing.getAxis()==Direction.Axis.Z)flip=!flip;
        Direction axis=Direction.from3DDataValue((facing.get3DDataValue()+(flip?4:2))%6);
        Direction rotated=facing.getClockWise(axis.getAxis());
        return side!=facing&&side!=rotated&&side!=rotated.getOpposite();
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        Direction face=state.getValue(FACING);double x=face.getStepX(),y=face.getStepY(),z=face.getStepZ();
        VoxelShape result=Shapes.box(Math.max(0,-x*.125),Math.max(0,-y*.125),Math.max(0,-z*.125),1-Math.max(0,x*.125),1-Math.max(0,y*.125),1-Math.max(0,z*.125));
        for(Direction wall:Direction.values())if(wall!=face)result=Shapes.or(result,switch(wall){case DOWN->box(0,0,0,16,5,16);case UP->box(0,11,0,16,16,16);case NORTH->box(0,0,0,16,16,5);case SOUTH->box(0,0,11,16,16,16);case WEST->box(0,0,0,5,16,16);case EAST->box(11,0,0,16,16,16);});
        return result;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ArcaneDropperBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Gadomancy.DROPPER_BE.get(),ArcaneDropperBlockEntity::tick);}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof ArcaneDropperBlockEntity dropper?dropper.comparator():0;}
}
