package forgottenrelics;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public final class RelicsConfig {
    public static final ForgeConfigSpec SPEC;
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();

    static {
        var b = new ForgeConfigSpec.Builder();
        VALUES.put(
                "forgottenKnowledgeOverridingEnabled",
                b.define("forgottenKnowledgeOverridingEnabled", false));
        VALUES.put(
                "forgottenKnowledgeOverrides",
                b.comment(
                                "Modern registry IDs:"
                                    + " AncientAegis[minecraft:diamond,minecraft:ender_eye]. Empty"
                                    + " brackets need no scans. NBT-sensitive overrides use the"
                                    + " data pack forgotten_knowledge.json instead.")
                        .defineListAllowEmpty(
                                "forgottenKnowledgeOverrides",
                                List.of(),
                                value -> value instanceof String));
        VALUES.put(
                "altTelekinesisAlgorithm",
                b.comment(
                                "True keeps vanilla movement slowdown while channeling telekinesis;"
                                        + " false restores the original unrestricted movement.")
                        .define("altTelekinesisAlgorithm", false));
        VALUES.put(
                "deificAmuletOnlyNegatesDebuffs",
                b.define("deificAmuletOnlyNegatesDebuffs", false));
        VALUES.put(
                "guardianAntiAbuseRadius",
                b.comment("Maximum 64 blocks to bound the liquid scan per guardian.")
                        .defineInRange("guardianAntiAbuseRadius", 16.0, 0.0, 64.0));
        VALUES.put(
                "guardianNotificationRadius",
                b.comment("Negative broadcasts to all players; zero disables notices.")
                        .defineInRange("guardianNotificationRadius", 64.0, -1.0, 32000.0));
        VALUES.put(
                "oblivionStoneHardCap",
                b.defineInRange("oblivionStoneHardCap", 64, 0, Integer.MAX_VALUE));
        VALUES.put(
                "oblivionStoneSoftCap",
                b.defineInRange("oblivionStoneSoftCap", 28, 0, Integer.MAX_VALUE));
        VALUES.put("voidGrimoireEnabled", b.define("voidGrimoireEnabled", true));
        VALUES.put(
                "outerLandsCheckrate",
                b.defineInRange("outerLandsCheckrate", 20, 0, Integer.MAX_VALUE));
        VALUES.put("outerLandsAntiAbuseEnabled", b.define("outerLandsAntiAbuseEnabled", true));
        VALUES.put("revelationModifier", b.defineInRange("revelationModifier", 1.0, 0.0, 32000.0));
        VALUES.put("telekinesisOnPlayers", b.define("telekinesisOnPlayers", true));
        VALUES.put(
                "fateTomeCooldownMIN",
                b.defineInRange("fateTomeCooldownMIN", 30, 0, Integer.MAX_VALUE));
        VALUES.put(
                "fateTomeCooldownMAX",
                b.defineInRange("fateTomeCooldownMAX", 90, 0, Integer.MAX_VALUE));
        VALUES.put(
                "advancedMiningCharmReach",
                b.defineInRange("advancedMiningCharmReach", 4.0, 0.0, 32000.0));
        VALUES.put("miningCharmReach", b.defineInRange("miningCharmReach", 2.0, 0.0, 32000.0));
        VALUES.put(
                "advancedMiningCharmBoost",
                b.defineInRange("advancedMiningCharmBoost", 3.0, 0.0, 32000.0));
        VALUES.put("miningCharmBoost", b.defineInRange("miningCharmBoost", 1.0, 0.0, 32000.0));
        VALUES.put(
                "nebulousCoreDodgeChance",
                b.defineInRange("nebulousCoreDodgeChance", 0.4, 0.0, 32000.0));
        VALUES.put(
                "ancientAegisDamageReduction",
                b.defineInRange("ancientAegisDamageReduction", 0.25, 0.0, 32000.0));
        VALUES.put("deificAmuletEffectImmunity", b.define("deificAmuletEffectImmunity", true));
        VALUES.put("deificAmuletInvincibility", b.define("deificAmuletInvincibility", true));
        VALUES.put(
                "darkSunRingDeflectChance",
                b.defineInRange("darkSunRingDeflectChance", 0.2, 0.0, 32000.0));
        VALUES.put(
                "darkSunRingDamageCap",
                b.defineInRange("darkSunRingDamageCap", 100.0, 0.0, 32000.0));
        VALUES.put("darkSunRingHealLimit", b.define("darkSunRingHealLimit", false));
        VALUES.put("interdimensionalMirror", b.define("interdimensionalMirror", true));
        VALUES.put(
                "shinyStoneCheckrate",
                b.defineInRange("shinyStoneCheckrate", 4, 0, Integer.MAX_VALUE));
        VALUES.put(
                "obeliskDrainerVisMult",
                b.defineInRange("obeliskDrainerVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("arcanumGenRate", b.defineInRange("arcanumGenRate", 1.0, 0.0, 32000.0));
        VALUES.put("soulTomeDivisor", b.defineInRange("soulTomeDivisor", 10.0, 0.0, 32000.0));
        VALUES.put("falseJusticeEnabled", b.define("falseJusticeEnabled", true));
        VALUES.put(
                "damageThunderpealDirect",
                b.defineInRange("damageThunderpealDirect", 24.0, 0.0, 32000.0));
        VALUES.put(
                "damageThunderpealBolt",
                b.defineInRange("damageThunderpealBolt", 16.0, 0.0, 32000.0));
        VALUES.put(
                "damageApotheosisDirect",
                b.defineInRange("damageApotheosisDirect", 100.0, 0.0, 32000.0));
        VALUES.put(
                "damageApotheosisImpact",
                b.defineInRange("damageApotheosisImpact", 75.0, 0.0, 32000.0));
        VALUES.put(
                "damageLunarFlareDirect",
                b.comment("The original entity actually dealt 100; its 72-point config was unused.")
                        .defineInRange("damageLunarFlareDirect", 100.0, 0.0, 32000.0));
        VALUES.put(
                "damageLunarFlareImpact",
                b.comment("The original entity actually dealt 75; its 40-point config was unused.")
                        .defineInRange("damageLunarFlareImpact", 75.0, 0.0, 32000.0));
        VALUES.put("paradoxDamageCap", b.defineInRange("paradoxDamageCap", 200.0, 0.0, 32000.0));
        VALUES.put(
                "telekinesisTomeDamageMIN",
                b.defineInRange("telekinesisTomeDamageMIN", 16.0, 0.0, 32000.0));
        VALUES.put(
                "telekinesisTomeDamageMAX",
                b.defineInRange("telekinesisTomeDamageMAX", 40.0, 0.0, 32000.0));
        VALUES.put(
                "nuclearFuryDamageMIN",
                b.defineInRange("nuclearFuryDamageMIN", 24.0, 0.0, 32000.0));
        VALUES.put(
                "nuclearFuryDamageMAX",
                b.defineInRange("nuclearFuryDamageMAX", 32.0, 0.0, 32000.0));
        VALUES.put(
                "crimsonSpellDamageMIN",
                b.defineInRange("crimsonSpellDamageMIN", 42.0, 0.0, 32000.0));
        VALUES.put(
                "crimsonSpellDamageMAX",
                b.defineInRange("crimsonSpellDamageMAX", 100.0, 0.0, 32000.0));
        VALUES.put(
                "chaosTomeDamageCap", b.defineInRange("chaosTomeDamageCap", 100.0, 0.0, 32000.0));
        VALUES.put(
                "eldritchSpellDamage", b.defineInRange("eldritchSpellDamage", 32.5, 0.0, 32000.0));
        VALUES.put(
                "eldritchSpellDamageEx",
                b.defineInRange("eldritchSpellDamageEx", 100.0, 0.0, 32000.0));
        VALUES.put("apotheosisVisMult", b.defineInRange("apotheosisVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("chaosTomeVisMult", b.defineInRange("chaosTomeVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "crimsonSpellVisMult", b.defineInRange("crimsonSpellVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "deificAmuletVisMult", b.defineInRange("deificAmuletVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("discordTomeVisMult", b.defineInRange("discordTomeVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "dormantArcanumVisMult",
                b.defineInRange("dormantArcanumVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "eldritchSpellVisMult", b.defineInRange("eldritchSpellVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("fateTomeVisMult", b.defineInRange("fateTomeVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("lunarFlaresVisMult", b.defineInRange("lunarFlaresVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("nuclearFuryVisMult", b.defineInRange("nuclearFuryVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "oblivionAmuletVisMult",
                b.defineInRange("oblivionAmuletVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("soulTomeVisMult", b.defineInRange("soulTomeVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "telekinesisTomeVisMult",
                b.defineInRange("telekinesisTomeVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "weatherStoneVisMult", b.defineInRange("weatherStoneVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("thunderpealVisMult", b.defineInRange("thunderpealVisMult", 1.0, 0.0, 32000.0));
        VALUES.put("overthrowerVisMult", b.defineInRange("overthrowerVisMult", 1.0, 0.0, 32000.0));
        VALUES.put(
                "voidGrimoireVisMult", b.defineInRange("voidGrimoireVisMult", 1.0, 0.0, 32000.0));
        SPEC = b.build();
    }

    private static Object setting(String key) {
        var value = Objects.requireNonNull(VALUES.get(key), key);
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static double value(String key) {
        return ((Number) setting(key)).doubleValue();
    }

    public static int integer(String key) {
        return (int) value(key);
    }

    public static boolean enabled(String key) {
        return (Boolean) setting(key);
    }

    public static List<String> knowledgeOverrides() {
        return ((List<?>) setting("forgottenKnowledgeOverrides"))
                .stream().map(String::valueOf).toList();
    }

    private RelicsConfig() {}
}
