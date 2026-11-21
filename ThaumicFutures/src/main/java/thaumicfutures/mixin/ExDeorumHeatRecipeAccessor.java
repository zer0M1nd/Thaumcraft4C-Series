package thaumicfutures.mixin;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.compat.jei.CrucibleHeatSourceRecipe", remap = false)
public interface ExDeorumHeatRecipeAccessor {
    @Accessor("blockState") BlockState futures$blockState();
    @Accessor("meltRate") int futures$meltRate();
}
