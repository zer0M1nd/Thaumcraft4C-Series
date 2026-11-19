package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public  class ArcaneLampBlock extends BaseEntityBlock {
    public enum Kind { LIGHT, GROWTH, FERTILITY }
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    private final Kind kind;
    public ArcaneLampBlock(Kind kind) {
        super(Properties.of().strength(3,10.2f).sound(SoundType.METAL).noOcclusion().lightLevel(state->kind==Kind.LIGHT || state.getValue(ACTIVE)?15:8));
        this.kind=kind; registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN).setValue(ACTIVE,kind==Kind.LIGHT));
    }
    public Kind kind() { return kind; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getClickedFace().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(4,2,4,12,14,12); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ArcaneLampBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide?null:createTickerHelper(type,TCArcane.ARCANE_LAMP_ENTITY.get(),ArcaneLampBlockEntity::tick); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos source,boolean moving) {
        if (!level.isClientSide && level.isEmptyBlock(pos.relative(state.getValue(FACING)))) level.destroyBlock(pos,true);
        super.neighborChanged(state,level,pos,block,source,moving);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!level.isClientSide && !next.is(this) && kind==Kind.LIGHT && level.getBlockEntity(pos) instanceof ArcaneLampBlockEntity lamp) lamp.removeLights();
        super.onRemove(state,level,pos,next,moving);
    }
}
