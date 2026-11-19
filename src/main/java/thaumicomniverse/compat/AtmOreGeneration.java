package thaumicomniverse.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.config.TCConfig;
import tc4revival.content.TCArcane;

/** Dimension-specific deposits; never runs in the overworld or a different mod's dimensions. */
public final class AtmOreGeneration {
    public static boolean applies(WorldGenLevel level) {
        return thaumicomniverse.RevivalFeatures.Group.ATM_ORES.enabled() && level.getLevel().dimension().location().getNamespace().equals("allthemodium");
    }

    public static void generate(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        if (!applies(level)) return;
        var random = context.random();
        var chunk = new ChunkPos(context.origin());
        for (int kind : new int[]{0, 7, 1}) {
            var feature = kind == 0 ? TCConfig.WorldFeature.CINNABAR_ORE
                    : kind == 7 ? TCConfig.WorldFeature.AMBER_ORE : TCConfig.WorldFeature.INFUSED_STONE;
            if (!TCConfig.SERVER.generates(feature)) continue;
            double expected = kind == 0 ? 18 : kind == 7 ? 20 : 8 * TCConfig.SERVER.infusedStoneDensityMultiplier.get();
            int count = (int) Math.floor(expected);
            if (random.nextDouble() < expected - count) count++;
            for (int attempt = 0; attempt < count; attempt++) {
                int x = chunk.getMinBlockX() + random.nextInt(16);
                int z = chunk.getMinBlockZ() + random.nextInt(16);
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                int bottom = level.getMinBuildHeight();
                int top = Math.min(level.getMaxBuildHeight(), surface);
                if (kind == 0) top = Math.min(top, 51);
                if (kind == 7) bottom = Math.max(bottom, top - 48);
                if (top <= bottom) continue; // A void dimension provides no substrate.
                int metadata = kind == 1 ? 1 + random.nextInt(6) : kind;
                Feature.ORE.place(new OreConfiguration(targets(metadata), kind == 1 ? 6 : 1),
                        level, context.chunkGenerator(), random,
                        new BlockPos(x, bottom + random.nextInt(top - bottom), z));
            }
        }
    }

    private static List<OreConfiguration.TargetBlockState> targets(int metadata) {
        var targets = new ArrayList<OreConfiguration.TargetBlockState>();
        for (var substrate : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.NETHERRACK, Blocks.END_STONE})
            targets.add(OreConfiguration.target(new BlockMatchTest(substrate), TCArcane.ore(metadata).defaultBlockState()));
        targets.add(OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                TCArcane.deepslateOre(metadata).defaultBlockState()));
        var ancient = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("allthemodium", "ancient_stone"));
        if (ancient != null && ancient != Blocks.AIR)
            targets.add(OreConfiguration.target(new BlockMatchTest(ancient), TCArcane.ore(metadata).defaultBlockState()));
        return targets;
    }
}
