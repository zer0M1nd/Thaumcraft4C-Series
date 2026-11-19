package arcanearteries;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common settings are read on the authoritative server. Recipe switches require a reload. */
public final class ArteriesConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue HARD_CRAFTING = B.define("hardMode.crafting", false);
    public static final ForgeConfigSpec.BooleanValue HARD_DAGGERS = B.comment("Halve normal self-sacrifice LP. Fixes the legacy double-fill bug.").define("hardMode.lessLp", false);
    public static final ForgeConfigSpec.BooleanValue REQUIRE_MANA = B.define("hardMode.botanicalKnifeRequiresMana", true);
    public static final ForgeConfigSpec.IntValue MANA_COST = B.defineInRange("hardMode.manaCost", 100, 0, 1000000);
    public static final ForgeConfigSpec.BooleanValue THAUMIC = B.define("daggers.thaumic", true);
    public static final ForgeConfigSpec.BooleanValue BOTANICAL = B.define("daggers.botanical", true);
    public static final ForgeConfigSpec.BooleanValue EVIL = B.define("daggers.evil", true);
    public static final ForgeConfigSpec.BooleanValue SELF = B.define("daggers.selfSacrifice", true);
    public static final ForgeConfigSpec.BooleanValue SACRIFICE = B.define("daggers.mobSacrifice", true);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_SELF = B.define("customSelf.enabled", false);
    public static final ForgeConfigSpec.IntValue SELF_LP = B.defineInRange("customSelf.lp", 100, 0, 1000000);
    public static final ForgeConfigSpec.IntValue FRAY_LP = B.defineInRange("customSelf.soulFrayLp", 10, 0, 1000000);
    public static final ForgeConfigSpec.IntValue HEALTH = B.defineInRange("customSelf.healthCost", 2, 1, 1000);
    public static final ForgeConfigSpec.ConfigValue<String> SELF_NAME = B.define("customSelf.name", "Razor Blade");
    public static final ForgeConfigSpec.ConfigValue<String> ORB_NAME = B.define("customSelf.orbName", "Special Orb");
    public static final ForgeConfigSpec.ConfigValue<String> SELF_DESCRIPTION = B.define("customSelf.description", "Slice 'n' Dice!");
    public static final ForgeConfigSpec.BooleanValue CUSTOM_MOB = B.define("customSacrifice.enabled", false);
    public static final ForgeConfigSpec.IntValue MOB_LP = B.defineInRange("customSacrifice.defaultLp", 500, 0, 1000000);
    public static final ForgeConfigSpec.IntValue VILLAGER_LP = B.defineInRange("customSacrifice.villagerLp", 2000, 0, 1000000);
    public static final ForgeConfigSpec.IntValue SLIME_LP = B.defineInRange("customSacrifice.slimeLp", 150, 0, 1000000);
    public static final ForgeConfigSpec.IntValue ENDERMAN_LP = B.defineInRange("customSacrifice.endermanLp", 200, 0, 1000000);
    public static final ForgeConfigSpec.IntValue ANIMAL_LP = B.defineInRange("customSacrifice.animalLp", 150, 0, 1000000);
    public static final ForgeConfigSpec.ConfigValue<String> MOB_NAME = B.define("customSacrifice.name", "Default Name");
    public static final ForgeConfigSpec.ConfigValue<String> MOB_DESCRIPTION = B.define("customSacrifice.description", "Default Message");
    public static final ForgeConfigSpec.BooleanValue SLATES = B.define("slates.enabled", true);
    public static final ForgeConfigSpec.BooleanValue UPGRADE = B.define("slates.reinforcedRecipe", true);
    public static final ForgeConfigSpec SPEC = B.build();
    public static java.util.List<ForgeConfigSpec.ConfigValue<?>> settings() {
        return java.util.List.of(HARD_CRAFTING, HARD_DAGGERS, REQUIRE_MANA, MANA_COST,
                THAUMIC, BOTANICAL, EVIL, SELF, SACRIFICE, CUSTOM_SELF, SELF_LP, FRAY_LP, HEALTH,
                SELF_NAME, ORB_NAME, SELF_DESCRIPTION, CUSTOM_MOB, MOB_LP, VILLAGER_LP, SLIME_LP,
                ENDERMAN_LP, ANIMAL_LP, MOB_NAME, MOB_DESCRIPTION, SLATES, UPGRADE);
    }
    @SuppressWarnings("unchecked")
    public static <T> T value(ForgeConfigSpec.ConfigValue<T> setting) {
        Object remote = ConfigSync.override(setting);
        return remote == null ? setting.get() : (T)remote;
    }
    private static final ForgeConfigSpec.Builder C = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue ORBS = C.comment("Use original orb artwork instead of self-sacrifice blades.").define("gentlePresentation", false);
    public static final ForgeConfigSpec CLIENT_SPEC = C.build();
    private ArteriesConfig() {}
}
