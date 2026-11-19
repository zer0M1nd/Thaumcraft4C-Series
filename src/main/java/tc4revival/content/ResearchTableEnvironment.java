package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.ResearchTableBonuses;

/** Research bonuses follow current block tags, fluids and enchanting support. */
public  class ResearchTableEnvironment implements ResearchTableBonuses.Environment {
    private final Level level;
    private final BlockPos pos;
    public ResearchTableEnvironment(Level level, BlockPos pos) { this.level = level; this.pos = pos; }
    @Override public int tableY() { return pos.getY(); }
    @Override public int height() { return level.getHeight(); }
    @Override public int minimumHeight() { return level.getMinBuildHeight(); }
    @Override public boolean darkCoveredNight() {
        return !level.isDay() && level.getMaxLocalRawBrightness(pos.above()) < 4 && !level.canSeeSky(pos.above());
    }
    @Override public ResearchTableBonuses.Source at(int x, int y, int z) {
        var target = pos.offset(x, y, z);
        // A table must not keep neighboring chunks loaded merely to collect a bonus.
        if (!level.hasChunkAt(target)) return ResearchTableBonuses.Source.NONE;
        var state = level.getBlockState(target);
        // TC4 checks specific ore/crystal metadata before generic legacy materials.
        var block=state.getBlock();
        if (block instanceof LegacyOreBlock ore && ore.metadata()>=1 && ore.metadata()<=6)
            return ResearchTableBonuses.Source.primal(primal(ore.metadata()-1),20);
        if (block instanceof CrystalClusterBlock crystal && crystal.type()!=ShardType.BALANCED)
            return ResearchTableBonuses.Source.primal(primal(crystal.type().metadata()),10);
        if (block instanceof BrainJarBlock) return ResearchTableBonuses.Source.brainJar();
        if (block instanceof LiquidDeathBlock || block instanceof PureWaterBlock)
            return ResearchTableBonuses.Source.primal("aqua",15);
        if (block instanceof TallowCandleBlock) return ResearchTableBonuses.Source.primal("ordo",20);
        if(state.getEnchantPowerBonus(level,target)>0)return ResearchTableBonuses.Source.bookshelf();
        return nativeSource(state);
    }
    private static String primal(int metadata) {
        return switch(metadata) { case 0 -> "aer"; case 1 -> "ignis"; case 2 -> "aqua"; case 3 -> "terra"; case 4 -> "ordo"; case 5 -> "perditio"; default -> throw new IllegalArgumentException("Invalid legacy primal metadata"); };
    }
    static ResearchTableBonuses.Source vanilla(String id) {
        var block=ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse(id));
        return block==null?ResearchTableBonuses.Source.NONE:nativeSource(block.defaultBlockState());
    }
    private static ResearchTableBonuses.Source nativeSource(net.minecraft.world.level.block.state.BlockState state) {
        var block=state.getBlock();
        if(state.is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER))return ResearchTableBonuses.Source.bookshelf();
        if(state.is(net.minecraft.tags.BlockTags.DIRT) || block instanceof net.minecraft.world.level.block.FarmBlock)return ResearchTableBonuses.Source.primal("terra",20);
        if(state.is(net.minecraft.tags.BlockTags.FIRE) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))return ResearchTableBonuses.Source.primal("ignis",20);
        if(state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))return ResearchTableBonuses.Source.primal("aqua",15);
        if(state.is(net.minecraft.tags.BlockTags.RAILS) || state.is(net.minecraft.tags.BlockTags.BUTTONS)
                || state.is(net.minecraft.tags.BlockTags.CLIMBABLE) || block instanceof net.minecraft.world.level.block.FlowerPotBlock
                || block instanceof net.minecraft.world.level.block.AbstractSkullBlock || block instanceof net.minecraft.world.level.block.TorchBlock
                || block instanceof net.minecraft.world.level.block.LeverBlock || block instanceof net.minecraft.world.level.block.RedStoneWireBlock
                || block instanceof net.minecraft.world.level.block.DiodeBlock || block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                || block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock || block instanceof net.minecraft.world.level.block.piston.MovingPistonBlock
                || block instanceof net.minecraft.world.level.block.TripWireBlock || block instanceof net.minecraft.world.level.block.TripWireHookBlock)
            return ResearchTableBonuses.Source.primal("ordo",20);
        return ResearchTableBonuses.Source.NONE;
    }
}
