package tc4revival.aspect.item;

import java.util.Map;

/** Semantic aspect bonuses for the current vanilla enchantment catalog and TC4 enchantments. */
public  class LegacyEnchantmentBonuses {
    private LegacyEnchantmentBonuses() {}
    private static final Map<String, String> ASPECTS = Map.ofEntries(
            Map.entry("minecraft:aqua_affinity", "aqua"), Map.entry("minecraft:bane_of_arthropods", "bestia"),
            Map.entry("minecraft:blast_protection", "tutamen"), Map.entry("minecraft:efficiency", "instrumentum"),
            Map.entry("minecraft:feather_falling", "volatus"), Map.entry("minecraft:fire_aspect", "ignis"),
            Map.entry("minecraft:fire_protection", "tutamen"), Map.entry("minecraft:flame", "ignis"),
            Map.entry("minecraft:fortune", "lucrum"), Map.entry("minecraft:infinity", "fabrico"),
            Map.entry("minecraft:knockback", "aer"), Map.entry("minecraft:looting", "lucrum"),
            Map.entry("minecraft:power", "telum"), Map.entry("minecraft:projectile_protection", "tutamen"),
            Map.entry("minecraft:protection", "tutamen"), Map.entry("minecraft:punch", "aer"),
            Map.entry("minecraft:respiration", "aer"), Map.entry("minecraft:sharpness", "telum"),
            Map.entry("minecraft:silk_touch", "permutatio"), Map.entry("minecraft:thorns", "telum"),
            Map.entry("minecraft:smite", "perditio"), Map.entry("minecraft:unbreaking", "terra"),
            Map.entry("minecraft:luck_of_the_sea", "lucrum"), Map.entry("minecraft:lure", "bestia"),
            Map.entry("minecraft:mending", "instrumentum"),Map.entry("minecraft:depth_strider", "motus"),
            Map.entry("minecraft:frost_walker", "gelum"),Map.entry("minecraft:binding_curse", "vinculum"),
            Map.entry("minecraft:vanishing_curse", "perditio"),Map.entry("minecraft:sweeping", "telum"),
            Map.entry("minecraft:loyalty", "vinculum"),Map.entry("minecraft:impaling", "telum"),
            Map.entry("minecraft:riptide", "aqua"),Map.entry("minecraft:channeling", "potentia"),
            Map.entry("minecraft:multishot", "telum"),Map.entry("minecraft:piercing", "telum"),
            Map.entry("minecraft:quick_charge", "motus"),Map.entry("minecraft:soul_speed", "spiritus"),
            Map.entry("minecraft:swift_sneak", "motus"),Map.entry("thaumcraft:haste", "motus"), Map.entry("thaumcraft:repair", "instrumentum"));
    public static String aspect(String enchantmentId) { return ASPECTS.get(enchantmentId); }
}
