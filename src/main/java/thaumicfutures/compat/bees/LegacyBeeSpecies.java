package thaumicfutures.compat.bees;

import com.google.gson.Gson;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.ForestryTaxa;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IGeneticRegistration;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public final class LegacyBeeSpecies {
    private record Product(String item, float chance, boolean specialty) {}
    private record Definition(String name, String legacy_name, String latin, String genus, int[] colors,
                              boolean glint, boolean dominant, boolean secret, String temperature, String humidity, Product[] products) {}
    private static final Definition[] DEFINITIONS = load();
    private LegacyBeeSpecies() {}
    private static Definition[] load() {
        try (var stream = LegacyBeeSpecies.class.getResourceAsStream("/data/thaumicfutures/forestry/legacy_species.json")) {
            if (stream == null) throw new IllegalStateException("Missing legacy bee definitions");
            return new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Definition[].class);
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    public static void genetics(IGeneticRegistration api) {
        var genera = new LinkedHashSet<String>();
        for (var definition : DEFINITIONS) genera.add("thaumicomniverse_legacy_" + definition.genus);
        for (String genus : genera) api.defineTaxon(ForestryTaxa.FAMILY_BEES, genus);
    }
    public static Set<ResourceLocation> register(IApicultureRegistration api) {
        var registered = new LinkedHashSet<ResourceLocation>();
        for (var definition : DEFINITIONS) {
            var id = ThaumicBeeMutations.parent(definition.name);
            var builder = api.registerSpecies(id, "thaumicomniverse_legacy_" + definition.genus,
                    definition.latin, definition.dominant, TextColor.fromRgb(definition.colors[0]));
            builder.setBody(TextColor.fromRgb(definition.colors[1])).setGlint(definition.glint).setSecret(definition.secret)
                    .setTemperature(TemperatureType.valueOf(definition.temperature)).setHumidity(HumidityType.valueOf(definition.humidity))
                    .setGenome(genome -> LegacyBeeGenomes.configure(definition.name, genome));
            for (var product : definition.products) {
                var stack = ThaumicBeeSpecies.stack(product.item);
                if (product.specialty) builder.addSpecialty(stack, product.chance);
                else builder.addProduct(stack, product.chance);
            }
            registered.add(id);
        }
        return Set.copyOf(registered);
    }
}
