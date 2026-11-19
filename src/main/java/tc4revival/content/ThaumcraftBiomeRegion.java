package tc4revival.content;

import com.mojang.datafixers.util.Pair;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

/** Ordinary forest climates, with the native river/coast/cave mappings left intact. */
public class ThaumcraftBiomeRegion extends Region {
    // Restrict to ordinary deciduous forest niches. Including every taiga/birch/dark
    // forest roughly doubles the old-pool frequency at the preserved 5:2 weights.
    private static final Set<ResourceKey<Biome>> FOREST_CLIMATES = Set.of(
            Biomes.FOREST, Biomes.FLOWER_FOREST);
    private final ResourceKey<Biome> biome;
    private int configuredWeight;

    public ThaumcraftBiomeRegion(ResourceKey<Biome> biome, int weight) {
        super(biome.location(), RegionType.OVERWORLD, weight);
        this.biome = biome;
        configureWeight(weight);
    }

    public void configureWeight(int weight) {
        // Bound old Integer.MAX_VALUE configs rather than overflow the shared region pool.
        configuredWeight = Math.max(0, Math.min(10_000, weight));
    }

    @Override public int getWeight() { return configuredWeight; }

    public static boolean usesForestClimate(ResourceKey<Biome> key) {
        return FOREST_CLIMATES.contains(key);
    }

    static void vanillaBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        new terrablender.api.ModifiedVanillaOverworldBuilder().build().forEach(mapper);
    }

    @Override public void addBiomes(Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // These are climate entries, not sampled world columns. The placeholder falls
        // back to the actual original source, preserving datapack biome mappings.
        vanillaBiomes(entry -> mapper.accept(Pair.of(entry.getFirst(),
                configuredWeight > 0 && usesForestClimate(entry.getSecond())
                        ? biome : DEFERRED_PLACEHOLDER)));
    }
}
