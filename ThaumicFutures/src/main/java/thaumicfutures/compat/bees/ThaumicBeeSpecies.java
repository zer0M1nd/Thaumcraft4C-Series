package thaumicfutures.compat.bees;

import forestry.api.IForestryApi;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.ForestryTaxa;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IBeeSpeciesBuilder;
import forestry.api.plugin.IGeneticRegistration;
import forestry.api.plugin.IGenomeBuilder;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import static forestry.api.genetics.alleles.ForestryAlleles.*;

/** The fifteen original TC branches, using modern Forestry genomes and production. */
public final class ThaumicBeeSpecies {
    private static final String GENUS = "thaumicomniverse_thaumic";
    private record Species(String name, String latin, int outline, int body, boolean glint, boolean dominant) {}
    private static final Species[] SPECIES = {
        new Species("air", "aether", 14276150, 0x999999, true, true),
        new Species("fire", "praefervidus", 15010571, 0x999999, true, true),
        new Species("water", "umidus", 3592153, 0x999999, true, true),
        new Species("earth", "sordida", 20736, 0x999999, true, true),
        new Species("order", "ordinatus", 11154172, 0x999999, true, true),
        new Species("chaos", "tenebrarum", 0xCCCCCC, 0x999999, true, false),
        new Species("vis", "arcanus saecula", 19609, 6774481, false, false),
        new Species("rejuvenating", "arcanus vitae", 9556185, 6774481, false, false),
        new Species("empowering", "tractus", 9895868, 6774481, false, true),
        new Species("nexus", "nexi", 1421231, 6774481, true, false),
        new Species("taint", "arcanus labe", 9516906, 6774481, false, false),
        new Species("pure", "arcanus puritatem", 14827365, 6774481, false, false),
        new Species("hungry", "omnique", 14460386, 6774481, false, false),
        new Species("wispy", "umbrabilis", 10270933, 14766646, false, false),
        new Species("void", "obscurus", 1575465, 4926068, false, false)
    };
    private ThaumicBeeSpecies() {}
    public static ResourceLocation id(String name) { return MagicBeesPlugin.key("bee_thaumic_" + name); }
    public static void genetics(IGeneticRegistration registration) {
        registration.defineTaxon(ForestryTaxa.FAMILY_BEES, GENUS);
    }
    public static void register(IApicultureRegistration registration) {
        for (Species species : SPECIES) {
            String name = species.name;
            IBeeSpeciesBuilder builder = registration.registerSpecies(id(name), GENUS, species.latin,
                    species.dominant, TextColor.fromRgb(species.outline));
            builder.setBody(TextColor.fromRgb(species.body)).setGlint(species.glint).setSecret(true)
                    .setTemperature(name.equals("fire") ? TemperatureType.HOT : name.equals("void") ? TemperatureType.ICY : TemperatureType.NORMAL)
                    .setHumidity(name.equals("fire") ? HumidityType.ARID : name.equals("water") ? HumidityType.DAMP : HumidityType.NORMAL)
                    .setGenome(genome -> genome(name, genome));
            products(name, builder);
        }
    }
    static void base(IGenomeBuilder genome) {
        genome.set(BeeChromosomes.SPEED, SPEED_SLOWEST);
        genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
        genome.set(BeeChromosomes.FERTILITY, FERTILITY_1);
        genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
        genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
        genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
        genome.set(BeeChromosomes.TOLERATES_RAIN, false);
        genome.set(BeeChromosomes.CAVE_DWELLING, false);
        genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
        genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
        genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
        flower(genome, "flowers_thaumic");
    }
    private static void genome(String name, IGenomeBuilder genome) {
        base(genome);
        if (java.util.Set.of("vis", "rejuvenating", "empowering", "nexus", "taint", "pure", "hungry").contains(name)) {
            genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_BOTH_1);
            genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_BOTH_1);
            flower(genome, "flowers_aura_nodes");
        }
        switch (name) {
            case "air" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FASTEST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTENED);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_LARGEST);
                genome.set(BeeChromosomes.TOLERATES_RAIN, true);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_MAXIMUM);
                effect(genome, "effect_speed");
            }
            case "fire" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FASTER);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_NORMAL);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_DOWN_3);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_UP_2);
                genome.set(BeeChromosomes.TOLERATES_RAIN, true);
                genome.set(BeeChromosomes.EFFECT, EFFECT_IGNITION);
            }
            case "water" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_LONG);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_DOWN_2);
                genome.set(BeeChromosomes.TOLERATES_RAIN, true);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_LARGE);
                effect(genome, "effect_cleanse");
            }
            case "earth" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOW);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_LONGER);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_DOWN_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, true);
                genome.set(BeeChromosomes.CAVE_DWELLING, true);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTEST);
                effect(genome, "effect_haste");
            }
            case "order" -> { genome.set(BeeChromosomes.SPEED, SPEED_NORMAL); genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_NORMAL); }
            case "chaos" -> genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
            case "vis" -> { genome.set(BeeChromosomes.SPEED, SPEED_SLOW); genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWER); }
            case "rejuvenating" -> { genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE); effect(genome, "effect_node_recharge"); }
            case "empowering" -> effect(genome, "effect_node_empower");
            case "nexus" -> effect(genome, "effect_node_repair");
            case "taint", "pure", "hungry" -> {
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                effect(genome, "effect_node_" + name);
                if (name.equals("pure")) genome.set(BeeChromosomes.POLLINATION, POLLINATION_AVERAGE);
                if (name.equals("hungry")) genome.set(BeeChromosomes.TERRITORY, TERRITORY_LARGEST);
            }
            case "wispy" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.TOLERATES_RAIN, true);
                effect(genome, "effect_wispy");
            }
            case "void" -> {
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.CAVE_DWELLING, true);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_UP_1);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.EFFECT, EFFECT_GLACIAL);
            }
            default -> throw new IllegalArgumentException(name);
        }
    }
    private static void flower(IGenomeBuilder genome, String name) {
        genome.set(BeeChromosomes.FLOWER_TYPE, IForestryApi.INSTANCE.getAlleleManager()
                .registryAllele(MagicBeesPlugin.key(name), BeeChromosomes.FLOWER_TYPE));
    }
    private static void effect(IGenomeBuilder genome, String name) {
        genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager()
                .registryAllele(MagicBeesPlugin.key(name), BeeChromosomes.EFFECT));
    }
    private static void products(String name, IBeeSpeciesBuilder builder) {
        for (String primal : MagicBeeProducts.PRIMALS) if (primal.equals(name)) {
            builder.addProduct(MagicBeeProducts.stack("bee_comb_" + name), .2f)
                    .addSpecialty(MagicBeeProducts.stack("bee_propolis_" + name), .18f);
            return;
        }
        float intellect = switch (name) {
            case "vis" -> .1f; case "rejuvenating", "taint" -> .18f; case "empowering" -> .14f;
            case "nexus" -> .25f; case "pure" -> .16f; case "hungry" -> .28f; default -> 0;
        };
        if (intellect > 0) builder.addProduct(MagicBeeProducts.stack("bee_comb_intellect"), intellect);
        switch (name) {
            case "empowering" -> builder.addSpecialty(stack("forestry:pollen_cluster_crystalline"), .02f);
            case "nexus" -> builder.addProduct(MagicBeeProducts.stack("bee_comb_temporal"), .12f)
                    .addSpecialty(stack("forestry:pollen_cluster_crystalline"), .02f);
            // Original code used craftingMaterial metadata1, which is pulsating mesh, despite the Propolis enum name.
            case "taint" -> builder.addProduct(stack("forestry:pulsating_mesh"), .213f);
            case "pure" -> builder.addSpecialty(MagicBeeProducts.stack("bee_comb_soul"), .19f);
            case "hungry" -> builder.addSpecialty(MagicBeeProducts.stack("bee_comb_temporal"), .195f);
            case "wispy" -> builder.addProduct(stack("forestry:bee_comb_silky"), .22f).addSpecialty(stack("forestry:silk_wisp"), .04f);
            case "void" -> builder.addProduct(stack("forestry:bee_comb_honey"), .1f).addSpecialty(stack("thaumcraft:void_nugget"), .155f);
        }
    }
    static ItemStack stack(String id) {
        var key = ResourceLocation.parse(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new IllegalStateException("Missing bee product " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }
}
