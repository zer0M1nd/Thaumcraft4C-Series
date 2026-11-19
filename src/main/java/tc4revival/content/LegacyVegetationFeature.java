package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.util.RandomSource;
import tc4revival.config.TCConfig;

/** The old per-chunk silverwood, greatwood and cinderpearl pass, in its original order. */
public  class LegacyVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public LegacyVegetationFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return decorate(context.level(),new ChunkPos(context.origin()),context.random(),true);
    }
    public static boolean decorate(WorldGenLevel level,ChunkPos chunk,RandomSource random,boolean newGeneration) {
        if (!TCConfig.SERVER.generates(TCConfig.WorldFeature.TREES)
                || !newGeneration && !TCConfig.SERVER.regenerates(TCConfig.WorldFeature.TREES)
                || level.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource
                || !level.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return false;
        boolean placed=false;
        if (random.nextInt(60)==3) {
            var pos=surface(level,chunk.getMinBlockX()+random.nextInt(16),chunk.getMinBlockZ()+random.nextInt(16));
            if (silverwoodBiome(level.getBiome(pos))) placed|=SilverwoodGrowth.natural(level,pos,random,7,4);
        }
        if (random.nextInt(25)==7) {
            var pos=surface(level,chunk.getMinBlockX()+random.nextInt(16),chunk.getMinBlockZ()+random.nextInt(16));
            if (LegacyBiomeProfiles.get(level.getBiome(pos)).greatwoodChance()>random.nextFloat())
                placed|=GreatwoodGrowth.natural(level,pos,random);
        }
        var pos=surface(level,chunk.getMinBlockX()+random.nextInt(16),chunk.getMinBlockZ()+random.nextInt(16));
        if (pos.getY()>level.getMaxBuildHeight()) return placed;
        var biome=level.getBiome(pos);
        if (LegacyBiomeProfiles.get(biome).sandySurface() && biome.value().getBaseTemperature()>1 && random.nextInt(30)==0)
            placed|=MagicalFlowerGrowth.scatter(level,pos,random,MagicalPlantBlock.Kind.CINDERPEARL)>0;
        return placed;
    }
    private static BlockPos surface(WorldGenLevel level,int x,int z) { return new BlockPos(x,LegacySurfaceHeight.at(level,x,z),z); }
    public static boolean silverwoodBiome(Holder<Biome> biome) {
        if (biome.is(TCBiomes.TAINTED_LAND) || biome.is(TCBiomes.MAGICAL_FOREST)) return false;
        // Those two old hill biome IDs no longer exist as distinct 1.20.1 biomes.
        return biome.is(Biomes.FOREST) || biome.is(Biomes.BIRCH_FOREST) || LegacyBiomeProfiles.get(biome).magical();
    }
}
