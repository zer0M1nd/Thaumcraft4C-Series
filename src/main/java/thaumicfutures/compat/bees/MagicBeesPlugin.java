package thaumicfutures.compat.bees;

import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public final class MagicBeesPlugin implements IForestryPlugin {
    public static ResourceLocation key(String name) { return new ResourceLocation("thaumicfutures", name); }
    @Override public ResourceLocation id() { return key("magic_bees"); }
    @Override public boolean shouldLoad() { return ModList.get().isLoaded("mobees"); }
    @Override public void registerGenetics(IGeneticRegistration registration) {
        ThaumicBeeSpecies.genetics(registration);
        LegacyBeeSpecies.genetics(registration);
    }
    @Override public void registerApiculture(IApicultureRegistration registration) {
        registration.registerFlowerType(key("flowers_thaumic"), new ThaumicFlowerType(false));
        registration.registerFlowerType(key("flowers_aura_nodes"), new ThaumicFlowerType(true));
        for (var action : NodeBeeEffect.Action.values())
            registration.registerBeeEffect(key("effect_node_" + action.name().toLowerCase(Locale.ROOT)), new NodeBeeEffect(action));
        for (var action : PlayerBeeEffect.Action.values())
            registration.registerBeeEffect(key("effect_" + action.name().toLowerCase(Locale.ROOT)), new PlayerBeeEffect(action));
        registration.registerBeeEffect(key("effect_wispy"), new WispyBeeEffect());
        for (String kind : java.util.List.of("batty", "ghastly", "wolf", "transmuting", "brainy", "chicken", "cow", "pig"))
            registration.registerBeeEffect(key("effect_legacy_" + kind), new LegacyBeeEffect(kind));
        ThaumicBeeSpecies.register(registration);
        var predecessors = LegacyBeeSpecies.register(registration);
        LegacyBeeMutations.register(registration);
        LegacyBeeHives.apiculture(registration);
        registration.modifySpecies(ResourceLocation.parse("mobees:bee_arcane"), species ->
                species.addSpecialty(MagicBeeProducts.stack("bee_drop_enchanted"), .09f));
        registration.modifySpecies(ResourceLocation.parse("mobees:bee_studious"), species ->
                species.addSpecialty(MagicBeeProducts.stack("bee_lore_fragment"), .02f));
        registration.modifySpecies(ResourceLocation.parse("mobees:bee_erudite"), species ->
                species.addSpecialty(MagicBeeProducts.stack("bee_lore_fragment"), .05f));
        if (ThaumicBeeMutations.register(registration, predecessors) != 15)
            throw new IllegalStateException("Incomplete thaumic bee mutation graph");
    }
}
