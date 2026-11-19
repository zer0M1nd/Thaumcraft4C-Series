package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** The eldritch crystal is supported by any non-air block on its attached side. */
public  class EldritchCrystalBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public EldritchCrystalBlock() {
        super(Properties.of().strength(.7f,1).sound(new net.minecraftforge.common.util.ForgeSoundType(1,1,
                TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL,TCSounds.CRYSTAL)).noOcclusion().lightLevel(state->7));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side=context.getClickedFace(); BlockPos support=context.getClickedPos().relative(side.getOpposite());
        if (!context.getLevel().getBlockState(support).isFaceSturdy(context.getLevel(),support,side)) return null;
        return defaultBlockState().setValue(FACING,side);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EldritchCrystalBlockEntity(pos,state); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos neighborPos,boolean moving) {
        if (!level.isClientSide && level.isEmptyBlock(pos.relative(state.getValue(FACING).getOpposite()))) level.destroyBlock(pos,true);
    }
}
