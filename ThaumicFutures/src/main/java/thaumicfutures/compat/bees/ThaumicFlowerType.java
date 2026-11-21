package thaumicfutures.compat.bees;

import forestry.api.apiculture.IFlowerType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.AuraNodeBlockEntity;
import tc4revival.content.EnergizedNodeBlockEntity;

public final class ThaumicFlowerType implements IFlowerType {
    private final boolean nodes;
    public ThaumicFlowerType(boolean nodes) { this.nodes = nodes; }
    @Override public boolean isDominant() { return false; }
    @Override public boolean isAcceptableFlower(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        if (nodes) return level.getBlockEntity(pos) instanceof AuraNodeBlockEntity
                || level.getBlockEntity(pos) instanceof EnergizedNodeBlockEntity;
        Block block = level.getBlockState(pos).getBlock();
        return block == plant("shimmerleaf") || block == plant("cinderpearl");
    }
    @Override public boolean plantRandomFlower(Level level, BlockPos pos, List<BlockState> flowers) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return false;
        if (nodes) return true; // Node flowers do not spread new aura nodes.
        if (level.isClientSide || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.below()) || !level.isEmptyBlock(pos))
            return false;
        BlockState ground = level.getBlockState(pos.below());
        Block block = ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK) ? plant("shimmerleaf")
                : ground.is(BlockTags.SAND) ? plant("cinderpearl") : Blocks.AIR;
        BlockState state = block.defaultBlockState();
        return block != Blocks.AIR && state.canSurvive(level, pos) && level.setBlock(pos, state, 3);
    }
    private static Block plant(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("thaumcraft", name));
    }
}
