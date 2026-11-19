package thaumicomniverse.compat.railcraft;

import mods.railcraft.world.level.block.manipulator.ManipulatorBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

public class EssentiaManipulatorBlock extends ManipulatorBlock<EssentiaManipulator> {
    public final boolean loader;
    public EssentiaManipulatorBlock(boolean loader){super(EssentiaManipulator.class,Properties.copy(Blocks.IRON_BLOCK).strength(3.5f).requiresCorrectToolForDrops());this.loader=loader;registerDefaultState(stateDefinition.any().setValue(POWERED,false));}
    @Override public Direction getFacing(BlockState state){return loader?Direction.DOWN:Direction.UP;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaManipulator(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ArcaneRail.MANIPULATOR.get(),EssentiaManipulator::serverTick);}
}
