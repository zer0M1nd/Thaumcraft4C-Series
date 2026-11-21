package thaumicfutures.compat.bees;

import forestry.api.IForestryApi;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.api.plugin.IGenomeBuilder;
import static forestry.api.genetics.alleles.ForestryAlleles.*;

/** Generated from reviewed inherited genome data by extract_bee_prerequisites.py. */
public final class LegacyBeeGenomes {
    private LegacyBeeGenomes() {}
    public static void configure(String name, IGenomeBuilder genome) {
        switch (name) {
            case "mystical", "sorcerous", "oblivion" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOWEST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, name.equals("oblivion") ? FERTILITY_4 : name.equals("sorcerous") ? FERTILITY_3 : FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, name.equals("oblivion") ? TOLERANCE_UP_2 : name.equals("sorcerous") ? TOLERANCE_DOWN_2 : TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, name.equals("oblivion") ? ACTIVITY_CATHEMERAL : ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, name.equals("sorcerous") ? TOLERANCE_UP_1 : TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, name.equals("oblivion") ? TRUE : FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, name.equals("oblivion") ? FLOWER_TYPE_END : FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, name.equals("oblivion") ? EFFECT_AGGRESSIVE : EFFECT_NONE);
            }
            case "attuned" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOWEST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_3);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "infernal" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOW);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORT);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_DOWN_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, TRUE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_NETHER);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_AGGRESSIVE);
            }
            case "eldritch" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOWER);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "charmed" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "enchanted" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_BOTH_1);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "supernatural" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_BOTH_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_BOTH_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "ethereal" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTENED);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_AVERAGE);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "windy" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FASTER);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_NORMAL);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_BOTH_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "firey" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_NORMAL);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_UP_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_DOWN_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_CACTI);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "watery" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_NORMAL);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_DOWN_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_UP_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_SNOW);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "earthy" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_NORMAL);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_LONG);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_BOTH_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "skulking" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, TRUE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "batty" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_LARGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_batty"), BeeChromosomes.EFFECT));
            }
            case "ghastly" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_1);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_BOTH_1);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, TRUE);
                genome.set(BeeChromosomes.CAVE_DWELLING, TRUE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_ghastly"), BeeChromosomes.EFFECT));
            }
            case "bigbad" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, TRUE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_wolf"), BeeChromosomes.EFFECT));
            }
            case "mutable" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOWEST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTEST);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_DOWN_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_UP_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, EFFECT_NONE);
            }
            case "transmuting" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_SLOWEST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTEST);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_DOWN_2);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_DIURNAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_UP_1);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_SLOWEST);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_transmuting"), BeeChromosomes.EFFECT));
            }
            case "brainy" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_1);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_brainy"), BeeChromosomes.EFFECT));
            }
            case "chicken" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_chicken"), BeeChromosomes.EFFECT));
            }
            case "beef" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_cow"), BeeChromosomes.EFFECT));
            }
            case "pork" -> {
                genome.set(BeeChromosomes.SPEED, SPEED_FAST);
                genome.set(BeeChromosomes.LIFESPAN, LIFESPAN_SHORTER);
                genome.set(BeeChromosomes.FERTILITY, FERTILITY_2);
                genome.set(BeeChromosomes.TEMPERATURE_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.ACTIVITY, ACTIVITY_CATHEMERAL);
                genome.set(BeeChromosomes.HUMIDITY_TOLERANCE, TOLERANCE_NONE);
                genome.set(BeeChromosomes.TOLERATES_RAIN, FALSE);
                genome.set(BeeChromosomes.CAVE_DWELLING, FALSE);
                genome.set(BeeChromosomes.FLOWER_TYPE, FLOWER_TYPE_VANILLA);
                genome.set(BeeChromosomes.POLLINATION, POLLINATION_FASTER);
                genome.set(BeeChromosomes.TERRITORY, TERRITORY_AVERAGE);
                genome.set(BeeChromosomes.EFFECT, IForestryApi.INSTANCE.getAlleleManager().registryAllele(MagicBeesPlugin.key("effect_legacy_pig"), BeeChromosomes.EFFECT));
            }
            default -> throw new IllegalArgumentException("Unknown legacy bee: " + name);
        }
    }
}
