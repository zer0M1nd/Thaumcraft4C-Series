package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;

/** Datapack-extensible soul heat, with extinguished campfires excluded. */
public class CrucibleHeat {
    public static final TagKey<Block> BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("thaumcraft:soul_crucible_heat"));
    public static final TagKey<Fluid> FLUIDS = TagKey.create(Registries.FLUID, ResourceLocation.parse("thaumcraft:soul_crucible_heat"));
    public static boolean soul(BlockState state) {
        return (state.is(BLOCKS) || state.getFluidState().is(FLUIDS))
                && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT));
    }
    public static boolean vanilla(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK) || (state.is(BlockTags.CAMPFIRES)
                && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT));
    }
    /** Shared by the crucible and the thaumatorium below its crucible base. */
    public static boolean heated(BlockState state) {
        return soul(state) || vanilla(state) || state.is(BlockTags.FIRE)
                || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                || state.getBlock() instanceof NitorBlock;
    }
}
