package thaumicomniverse.compat.create;
import com.simibubi.create.content.redstone.thresholdSwitch.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.*;
/** Old saves retain their registered block; the host's percentage UI drives its native hysteresis. */
public class EssentiaSwitch extends ThresholdSwitchBlock {
    public EssentiaSwitch(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2).noOcclusion());}
    @Override public BlockEntityType<? extends ThresholdSwitchBlockEntity> getBlockEntityType(){return ArcaneCreateContent.SWITCH_TILE.get();}
    public static class Tile extends ThresholdSwitchBlockEntity {
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.SWITCH_TILE.get(),pos,state);}
        public MutableComponent format(float value,boolean stacks){return Component.literal(Math.round(value*100)+"%");}
    }
}
