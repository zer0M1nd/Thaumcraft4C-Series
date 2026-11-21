package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Six elemental clusters and the six-colour mixed cluster; facing points away from support. */
public  class CrystalClusterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    private final ShardType type;
    public CrystalClusterBlock(ShardType type) {
        super(Properties.of().strength(.7f,.6f).noOcclusion().lightLevel(state->7)
                .sound(new net.minecraftforge.common.util.ForgeSoundType(1,1,TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL)));
        this.type=type; registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));
    }
    public ShardType type() { return type; }
    public static String path(ShardType type) { return type.crystalClusterPath(); }
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (random.nextInt(17)==0) tc4revival.client.ClientFocusEffects.crystalSpark(level,pos,type);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=defaultBlockState().setValue(FACING,context.getClickedFace());
        return state.canSurvive(context.getLevel(),context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var face=state.getValue(FACING); var support=pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level,support,face);
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos adjacent) {
        if (!state.canSurvive(level,pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,side,neighbor,level,pos,adjacent);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case UP->box(2,0,2,14,12,14);case DOWN->box(2,4,2,14,16,14);
            case NORTH->box(2,2,4,14,14,16);case SOUTH->box(2,2,0,14,14,12);
            case EAST->box(0,2,2,12,14,14);case WEST->box(4,2,2,16,14,14);
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new CrystalClusterBlockEntity(pos,state); }
}
