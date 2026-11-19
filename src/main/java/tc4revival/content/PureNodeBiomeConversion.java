package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Pure nodes clean tainted columns; a node embedded in magical wood also spreads forest. */
 class PureNodeBiomeConversion {
    private PureNodeBiomeConversion() {}
    static void tick(ServerLevel level,BlockPos node) {
        if (level.dimension().equals(Level.NETHER) || level.dimension().equals(Level.END)) return;
        var random=level.random;
        var target=node.offset(random.nextInt(8)-random.nextInt(8),0,random.nextInt(8)-random.nextInt(8));
        convert(level,node,target);
    }
    static boolean convert(ServerLevel level,BlockPos node,BlockPos target) {
        if (level.dimension().equals(Level.NETHER) || level.dimension().equals(Level.END)
                || !level.hasChunkAt(node) || !level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)) return false;
        var biome=level.getBiomeManager().getNoiseBiomeAtPosition(target);
        if (biome.is(TCBiomes.MAGICAL_FOREST)||biome.is(TCBiomes.MAGICAL_FORESTS)) return false;
        if (!biome.is(TCBiomes.TAINTED_LAND) && !(level.getBlockState(node).getBlock() instanceof MagicalLogBlock)) return false;
        return WorldBiomeChanges.changeNodeBiome(level,node,target,TCBiomes.MAGICAL_FOREST);
    }
}
