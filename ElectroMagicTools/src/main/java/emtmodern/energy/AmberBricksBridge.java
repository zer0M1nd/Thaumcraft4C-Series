package emtmodern.energy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
/** Missing old TC4 cosmetic opaque metadata 1, used by the original UU infusion chain. */
public class AmberBricksBridge extends Block {
    public AmberBricksBridge(){super(Properties.of().strength(1.5f,5).sound(SoundType.STONE).noOcclusion().isRedstoneConductor((state,level,pos)->true));}
    @Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos){return 3;}
    @Override public boolean skipRendering(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.block.state.BlockState adjacent,net.minecraft.core.Direction face){return adjacent.is(this)||super.skipRendering(state,adjacent,face);}
    @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion){return false;}
}
