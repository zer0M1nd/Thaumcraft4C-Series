package thaumicfutures.compat.bees;

import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IMutationBuilder;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Native mutation definitions. Additional parent IDs are supplied by the prerequisite branch. */
public final class ThaumicBeeMutations {
    public static final TagKey<Biome> MAGICAL = TagKey.create(Registries.BIOME, MagicBeesPlugin.key("magical"));
    private ThaumicBeeMutations() {}

    public static int register(IApicultureRegistration api, Set<ResourceLocation> additionalParents) {
        int count = 0;
        count += add(api, additionalParents, "air", "windy", "windy", 8, resource("air_crystal_cluster"));
        count += add(api, additionalParents, "fire", "firey", "firey", 8, resource("fire_crystal_cluster"));
        count += add(api, additionalParents, "water", "watery", "watery", 8, resource("water_crystal_cluster"));
        count += add(api, additionalParents, "earth", "earthy", "earthy", 8, resource("earth_crystal_cluster"));
        count += add(api, additionalParents, "order", "ethereal", "arcane", 8, resource("order_crystal_cluster"));
        count += add(api, additionalParents, "chaos", "ethereal", "supernatural", 8, resource("entropy_crystal_cluster"));
        count += add(api, additionalParents, "vis", "ethereal", "infernal", 9,
                mutation -> mutation.addMutationCondition(MoonMutationCondition.require(6, 2)));
        count += add(api, additionalParents, "rejuvenating", "attuned", "tc_vis", 8, mutation -> {});
        count += add(api, additionalParents, "empowering", "tc_vis", "tc_rejuvenating", 6,
                mutation -> mutation.addMutationCondition(new MoonMutationCondition(0, 0, 1.322f, false)));
        count += add(api, additionalParents, "nexus", "tc_rejuvenating", "tc_empowering", 10,
                mutation -> { mutation.restrictBiomeType(MAGICAL); resource("aura_node").accept(mutation); });
        count += add(api, additionalParents, "taint", "transmuting", "tc_empowering", 11,
                mutation -> mutation.addMutationCondition(MoonMutationCondition.require(4, 4)));
        count += add(api, additionalParents, "pure", "transmuting", "tc_rejuvenating", 8,
                mutation -> mutation.restrictBiomeType(MAGICAL).addMutationCondition(MoonMutationCondition.require(4, 4)));
        count += add(api, additionalParents, "hungry", "bigbad", "tc_vis", 20, mutation -> {});
        count += add(api, additionalParents, "wispy", "ethereal", "ghastly", 9,
                mutation -> mutation.addMutationCondition(MoonMutationCondition.require(3, 5)));
        count += add(api, additionalParents, "void", "iron", "tc_taint", 5,
                mutation -> mutation.restrictBiomeType(MAGICAL).requireNight());
        return count;
    }

    public static ResourceLocation parent(String name) {
        if (name.startsWith("tc_")) return ThaumicBeeSpecies.id(name.substring(3));
        if (Set.of("arcane", "mysterious", "unusual", "iron", "pupil").contains(name))
            return ResourceLocation.fromNamespaceAndPath("mobees", "bee_" + name);
        return MagicBeesPlugin.key("bee_legacy_" + name);
    }
    private static boolean available(String name, Set<ResourceLocation> additional) {
        return name.startsWith("tc_") || parent(name).getNamespace().equals("mobees") || additional.contains(parent(name));
    }
    private static int add(IApicultureRegistration api, Set<ResourceLocation> additional,
            String child, String first, String second, int percent, Consumer<IMutationBuilder> conditions) {
        if (!available(first, additional) || !available(second, additional)) return 0;
        api.modifySpecies(ThaumicBeeSpecies.id(child), species -> species.addMutations(mutations -> {
            if (mutations.get(parent(first), parent(second)) == null)
                conditions.accept(mutations.add(parent(first), parent(second), percent / 100f).addMutationCondition(new FeatureMutationCondition()));
        }));
        return 1;
    }
    private static Consumer<IMutationBuilder> resource(String path) {
        return mutation -> {
            var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
            if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalStateException("Missing bee mutation resource: " + id);
            // Old metadata selected the crystal element, not its modern attachment orientation.
            var block = ForgeRegistries.BLOCKS.getValue(id);
            mutation.requireResource(block.getStateDefinition().getPossibleStates().toArray(BlockState[]::new));
        };
    }
}
