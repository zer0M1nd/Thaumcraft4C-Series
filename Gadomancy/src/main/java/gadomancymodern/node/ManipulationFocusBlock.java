package gadomancymodern.node;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class ManipulationFocusBlock extends BaseEntityBlock {
    public final boolean portal;
    public ManipulationFocusBlock(boolean portal){super(Properties.of().strength(3,25).noOcclusion().sound(SoundType.STONE));this.portal=portal;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FocusEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    public static class FocusEntity extends BlockEntity {public FocusEntity(BlockPos pos,BlockState state){super(Gadomancy.FOCUS_BE.get(),pos,state);}public boolean portal(){return getBlockState().getBlock() instanceof ManipulationFocusBlock b&&b.portal;}}
}
