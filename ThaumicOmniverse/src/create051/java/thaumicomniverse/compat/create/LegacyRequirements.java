package thaumicomniverse.compat.create;
import java.util.function.BiFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
public final class LegacyRequirements {
    public static final LegacyTransforms.Table<Block,BiFunction<BlockState,BlockEntity,ItemRequirement>> BLOCKS=new LegacyTransforms.Table<>();
}
