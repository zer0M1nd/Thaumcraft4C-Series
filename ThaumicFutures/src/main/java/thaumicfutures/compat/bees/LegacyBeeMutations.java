package thaumicfutures.compat.bees;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.IMutationCondition;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IMutationBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class LegacyBeeMutations {
    private LegacyBeeMutations() {}
    public static void register(IApicultureRegistration api) {
        for (String root : new String[]{"attuned", "unusual", "mystical", "sorcerous"}) {
            for (String mundane : new String[]{"forest", "meadows", "modest", "wintry", "tropical", "marshy"})
                add(api, "forestry:common", root, "forestry:" + mundane, 15, mutation -> {});
            add(api, "forestry:cultivated", root, "forestry:common", 12, mutation -> {});
            add(api, "eldritch", root, "forestry:cultivated", 12, mutation -> {});
        }
        add(api, "charmed", "forestry:cultivated", "eldritch", 10, mutation -> {});
        add(api, "enchanted", "eldritch", "charmed", 8, mutation -> {});
        add(api, "supernatural", "charmed", "enchanted", 8,
                mutation -> mutation.addMutationCondition(new MoonMutationCondition(1, 3, 1.2f, false)));
        add(api, "ethereal", "arcane", "supernatural", 7, mutation -> {});
        add(api, "windy", "supernatural", "ethereal", 14,
                mutation -> mutation.addMutationCondition(new LeavesBelow()));
        add(api, "watery", "supernatural", "ethereal", 14, mutation -> mutation.requireResource(Blocks.WATER.defaultBlockState()));
        add(api, "earthy", "supernatural", "ethereal", 14, mutation -> mutation.requireResource(Blocks.CLAY.defaultBlockState()));
        add(api, "firey", "supernatural", "ethereal", 14, mutation -> mutation.requireResource(Blocks.LAVA.defaultBlockState()));
        add(api, "skulking", "forestry:modest", "eldritch", 12, mutation -> {});
        add(api, "batty", "skulking", "windy", 9, mutation -> {});
        add(api, "ghastly", "batty", "ethereal", 9, mutation -> {});
        add(api, "bigbad", "skulking", "mysterious", 7,
                mutation -> mutation.addMutationCondition(MoonMutationCondition.require(0, 0)));
        add(api, "mutable", "unusual", "eldritch", 12, mutation -> {});
        add(api, "transmuting", "unusual", "mutable", 9, mutation -> {});
        add(api, "brainy", "skulking", "pupil", 9, mutation -> {});
        add(api, "chicken", "forestry:common", "skulking", 12,
                mutation -> mutation.restrictBiomeType(net.minecraft.tags.BiomeTags.IS_FOREST));
        add(api, "beef", "forestry:common", "skulking", 12,
                mutation -> mutation.restrictBiomeType(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.BIOME, MagicBeesPlugin.key("plains"))));
        add(api, "pork", "forestry:common", "skulking", 12,
                mutation -> mutation.restrictBiomeType(net.minecraft.tags.BiomeTags.IS_MOUNTAIN));
    }
    private static ResourceLocation id(String name) {
        return name.startsWith("forestry:") ? ResourceLocation.fromNamespaceAndPath("forestry", "bee_" + name.substring(9))
                : ThaumicBeeMutations.parent(name);
    }
    private static void add(IApicultureRegistration api, String child, String first, String second, int percent,
                            Consumer<IMutationBuilder> condition) {
        api.modifySpecies(id(child), species -> species.addMutations(mutations -> {
            // Forestry replays species modifications while building species and again for mutations.
            if (mutations.get(id(first), id(second)) == null)
                condition.accept(mutations.add(id(first), id(second), percent / 100f).addMutationCondition(new FeatureMutationCondition()));
        }));
    }
    private static final class LeavesBelow implements IMutationCondition {
        @Override public float modifyChance(Level level, BlockPos pos, IMutation<?> mutation,
                IGenome first, IGenome second, IClimateProvider climate, float chance) {
            BlockPos below = pos.below();
            while (below.getY() >= level.getMinBuildHeight() && level.hasChunkAt(below)
                    && level.getBlockEntity(below) instanceof IBeeHousing) below = below.below();
            return level.hasChunkAt(below) && level.getBlockState(below).is(BlockTags.LEAVES) ? chance : 0;
        }
        @Override public Component getDescription() { return Component.translatable("omniverse.bees.leaves_below"); }
    }
}
