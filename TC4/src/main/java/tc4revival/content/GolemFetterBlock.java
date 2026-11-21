package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Old cosmetic-solid states 9 and 10, changed only by neighboring redstone updates. */
public  class GolemFetterBlock extends Block {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public GolemFetterBlock() { super(Properties.of().strength(2,6).requiresCorrectToolForDrops().sound(SoundType.STONE)); registerDefaultState(stateDefinition.any().setValue(POWERED,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(POWERED); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos neighborPos,boolean moving) {
        boolean powered=level.hasNeighborSignal(pos);
        if (state.getValue(POWERED)!=powered) level.setBlock(pos,state.setValue(POWERED,powered),3);
        super.neighborChanged(state,level,pos,neighbor,neighborPos,moving);
    }
}
