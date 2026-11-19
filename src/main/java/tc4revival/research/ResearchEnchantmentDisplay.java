package tc4revival.research;

import java.util.Map;

/** Legacy book names mapped to modern vanilla translation keys; not enchantment registration. */
public  class ResearchEnchantmentDisplay {
    private static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("protect.all", "protection"), Map.entry("protect.fire", "fire_protection"),
            Map.entry("protect.fall", "feather_falling"), Map.entry("protect.explosion", "blast_protection"),
            Map.entry("protect.projectile", "projectile_protection"), Map.entry("oxygen", "respiration"),
            Map.entry("waterWorker", "aqua_affinity"), Map.entry("thorns", "thorns"),
            Map.entry("damage.all", "sharpness"), Map.entry("damage.undead", "smite"),
            Map.entry("damage.arthropods", "bane_of_arthropods"), Map.entry("knockback", "knockback"),
            Map.entry("fire", "fire_aspect"), Map.entry("lootBonus", "looting"),
            Map.entry("digging", "efficiency"), Map.entry("untouching", "silk_touch"),
            Map.entry("durability", "unbreaking"), Map.entry("lootBonusDigger", "fortune"),
            Map.entry("arrowDamage", "power"), Map.entry("arrowKnockback", "punch"),
            Map.entry("arrowFire", "flame"), Map.entry("arrowInfinite", "infinity"));
    private ResearchEnchantmentDisplay() {}
    public record Cost(int level, int experienceLevels, Map<String, Integer> aspects) {}
    public static Cost cost(ResearchPageRecipes.Enchanting recipe, long milliseconds) {
        if (recipe.maxLevel() < 1) throw new IllegalArgumentException("Invalid enchantment maximum level");
        int level = 1 + (int) Math.floorMod(milliseconds / 1000, recipe.maxLevel());
        var costs = new java.util.LinkedHashMap<String, Integer>();
        recipe.aspects().forEach((tag, amount) -> costs.put(tag, Math.multiplyExact(amount, level)));
        return new Cost(level, Math.multiplyExact(recipe.baseXp(), level), ResearchPage.ordered(costs));
    }
    public static String nameKey(String legacy) {
        String shortName = legacy.startsWith("enchantment.") ? legacy.substring("enchantment.".length()) : legacy;
        String modern = VANILLA.get(shortName);
        return modern == null ? legacy : "enchantment.minecraft." + modern;
    }
}
