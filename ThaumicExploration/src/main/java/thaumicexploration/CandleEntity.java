package thaumicexploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Position-only render anchor; floating candles require no server ticking. */
public class CandleEntity extends BlockEntity {
    public CandleEntity(BlockPos pos,BlockState state){super(ExplorationMod.CANDLE.get(),pos,state);}
}
