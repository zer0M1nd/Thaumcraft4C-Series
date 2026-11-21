package tc4revival.research;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchEnchantmentDisplayTest {
    private ResearchPageRecipes.Enchanting recipe(int maxLevel) {
        return new ResearchPageRecipes.Enchanting("TEST", 0, "enchantment.protect.all", maxLevel,
                5, 3, Map.of("aer", 8, "ordo", 4), new ResearchPage.Sequence(List.of()));
    }
    @Test void levelsAndBothCostsCycleTogetherAtSecondBoundaries() {
        var recipe = recipe(3);
        assertEquals(new ResearchEnchantmentDisplay.Cost(1, 5, Map.of("aer", 8, "ordo", 4)),
                ResearchEnchantmentDisplay.cost(recipe, 999));
        assertEquals(new ResearchEnchantmentDisplay.Cost(2, 10, Map.of("aer", 16, "ordo", 8)),
                ResearchEnchantmentDisplay.cost(recipe, 1000));
        assertEquals(new ResearchEnchantmentDisplay.Cost(3, 15, Map.of("aer", 24, "ordo", 12)),
                ResearchEnchantmentDisplay.cost(recipe, 2999));
        assertEquals(1, ResearchEnchantmentDisplay.cost(recipe, 3000).level());
        assertEquals(Map.of("aer", 8, "ordo", 4), recipe.aspects());
        assertThrows(UnsupportedOperationException.class,
                () -> ResearchEnchantmentDisplay.cost(recipe, 1000).aspects().put("aer", 0));
    }
    @Test void singleLevelEnchantmentsStayFixedAndInvalidMaximumIsRejected() {
        assertEquals(1, ResearchEnchantmentDisplay.cost(recipe(1), Long.MAX_VALUE).level());
        assertThrows(IllegalArgumentException.class, () -> ResearchEnchantmentDisplay.cost(recipe(0), 0));
    }
    @Test void observedEnchantmentsHaveModernVanillaOrPreservedModNames() throws Exception {
        try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(getClass()
                .getResourceAsStream("/research-page-recipes.json")), java.nio.charset.StandardCharsets.UTF_8)) {
            var recipes = ResearchPageRecipes.decode(com.google.gson.JsonParser.parseReader(reader).getAsJsonArray());
            int count = 0;
            for (var entry : recipes.entries().values()) {
                if (!(entry.definition() instanceof ResearchPageRecipes.Enchanting enchanting)) continue;
                count++;
                String key = ResearchEnchantmentDisplay.nameKey(enchanting.enchantmentName());
                assertTrue(key.startsWith("enchantment.minecraft.")
                        || key.equals("enchantment.haste") || key.equals("enchantment.repair"), entry.key());
                var last = ResearchEnchantmentDisplay.cost(enchanting, (enchanting.maxLevel() - 1L) * 1000);
                assertEquals(enchanting.maxLevel(), last.level());
                assertEquals(enchanting.baseXp() * enchanting.maxLevel(), last.experienceLevels());
            }
            assertTrue(count > 0);
        }
        assertEquals("enchantment.minecraft.fire_aspect", ResearchEnchantmentDisplay.nameKey("enchantment.fire"));
        assertEquals("enchantment.minecraft.flame", ResearchEnchantmentDisplay.nameKey("enchantment.arrowFire"));
        assertEquals("enchantment.repair", ResearchEnchantmentDisplay.nameKey("enchantment.repair"));
    }
}
