package witchinggadgets.api;
import net.minecraft.core.BlockPos;import net.minecraft.resources.ResourceKey;import net.minecraft.world.level.Level;import net.minecraft.world.level.biome.Biome;import net.minecraft.world.item.ItemStack;import tc4revival.aspect.Aspect;
/** Implement on a block or block entity above a terraformer. No enum restricts addon foci. */
public interface ITerraformFocus {
 Aspect requiredAspect(Level level,BlockPos pos);
 ResourceKey<Biome> createdBiome(Level level,BlockPos pos);
 ItemStack displayedBlock(Level level,BlockPos pos);
}
