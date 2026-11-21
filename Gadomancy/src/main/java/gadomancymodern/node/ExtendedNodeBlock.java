package gadomancymodern.node;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class ExtendedNodeBlock extends tc4revival.content.AuraNodeBlock {
    @Override public String getDescriptionId(){return tc4revival.content.TCArcane.AURA_NODE.get().getDescriptionId();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ExtendedNodeEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.EXTENDED_NODE_BE.get(),(world,pos,block,node)->node.tick());}
}
