package thaumicomniverse.content;

import tc4revival.content.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** Shares the original selectable arms, label interaction and dimensions, but not suction flow. */
public class ConvenientTubeBlock extends EssentiaTubeBlock {
    public ConvenientTubeBlock(EssentiaTubeFlow.Kind kind) { super(kind); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block neighbor,BlockPos changed,boolean moved){
        super.neighborChanged(state,level,pos,neighbor,changed,moved);
        if(level.getBlockEntity(pos) instanceof ConvenientTubeBlockEntity tube)tube.refreshAttachments();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ConvenientTubeBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,ModernAlchemy.CONVENIENT_TUBE_ENTITY.get(),(world,pos,block,tube)->tube.tick());
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moved) {
        if(!state.is(replacement.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ConvenientTubeBlockEntity tube)
            tube.dropAttachments();
        super.onRemove(state,level,pos,replacement,moved);
    }
}
