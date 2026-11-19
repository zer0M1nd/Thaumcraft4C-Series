package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public  class EssentiaMirrorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public EssentiaMirrorBlock() { super(Properties.of().strength(1,10).sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion().noCollission()); registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getClickedFace()); }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var face=state.getValue(FACING); var support=pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level,support,face);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        if (!level.isClientSide && !canSurvive(state,level,pos)) level.destroyBlock(pos,true);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case DOWN->Block.box(0,15,0,16,16,16); case UP->Block.box(0,0,0,16,1,16);
            case NORTH->Block.box(0,0,15,16,16,16); case SOUTH->Block.box(0,0,0,16,16,1);
            case WEST->Block.box(15,0,0,16,16,16); case EAST->Block.box(0,0,0,1,16,16);
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EssentiaMirrorBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return type==TCArcane.ESSENTIA_MIRROR_ENTITY.get() ? (world,pos,block,entity)->((EssentiaMirrorBlockEntity)entity).tick() : null;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof EssentiaMirrorBlockEntity mirror) mirror.invalidateLink();
        super.onRemove(state,level,pos,replacement,moving);
    }
}
