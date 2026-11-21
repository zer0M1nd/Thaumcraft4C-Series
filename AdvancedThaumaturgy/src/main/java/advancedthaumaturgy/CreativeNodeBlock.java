package advancedthaumaturgy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class CreativeNodeBlock extends BaseEntityBlock {
    public CreativeNodeBlock(){super(Properties.of().strength(-1,3600000).noCollission().noOcclusion().lightLevel(s->12));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CreativeNodeEntity(p,s);}
}
