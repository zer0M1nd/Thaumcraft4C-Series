package timodern.mixin;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=Block.class,remap=false)
public abstract class InfusedBlockMixin implements thaumcraft.api.crafting.IInfusionStabiliser {
    public boolean canStabiliseInfusion(Level world,BlockPos pos){
        // This mixin applies to every block: absence of Ordo must not veto ordinary stabilizers.
        return world.getBlockState(pos).is(tc4revival.content.InfusionLayout.STABILIZERS)
                || timodern.InfusionData.has(world,pos,"ordo");
    }
}
