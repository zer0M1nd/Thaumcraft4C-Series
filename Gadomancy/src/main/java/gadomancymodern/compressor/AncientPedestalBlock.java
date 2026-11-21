package gadomancymodern.compressor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** The original ancient pedestal has the ordinary TC pedestal inventory and infusion behavior. */
public class AncientPedestalBlock extends tc4revival.content.ArcanePedestalBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AncientPedestalEntity(pos,state);}
}
