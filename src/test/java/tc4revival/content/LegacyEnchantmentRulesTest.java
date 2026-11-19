package tc4revival.content;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyEnchantmentRulesTest {
    @Test void maximumCostsUseEachEnchantmentsOwnMinimumPlusFifty() {
        var haste = LegacyEnchantmentRules.Kind.HASTE;
        assertEquals(15, haste.minCost(1)); assertEquals(33, haste.minCost(3));
        assertEquals(65, haste.maxCost(1)); assertEquals(83, haste.maxCost(3)); assertEquals(3, haste.weight());
        var repair = LegacyEnchantmentRules.Kind.REPAIR;
        assertEquals(20, repair.minCost(1)); assertEquals(30, repair.minCost(2));
        assertEquals(70, repair.maxCost(1)); assertEquals(80, repair.maxCost(2)); assertEquals(2, repair.weight());
    }
    @Test void movementConditionsMultiplyIndependently() {
        assertEquals(.045f, LegacyEnchantmentRules.hasteAcceleration(3, true, false, false, false), .000001f);
        assertEquals(.01125f, LegacyEnchantmentRules.hasteAcceleration(3, true, false, true, true), .000001f);
        assertEquals(0, LegacyEnchantmentRules.hasteAcceleration(3, false, false, false, false));
        assertEquals(0, LegacyEnchantmentRules.hasteAcceleration(3, true, true, false, false));
    }
    @Test void repairRoundsBeforeMultiplyingAndCapsCommandLevels() {
        var primals = Map.of("aer", 7, "ordo", 2);
        assertEquals(Map.of("aer", 3, "ordo", 2), LegacyEnchantmentRules.repairCost(primals, 1));
        assertEquals(Map.of("aer", 6, "ordo", 4), LegacyEnchantmentRules.repairCost(primals, 2));
        assertEquals(LegacyEnchantmentRules.repairCost(primals, 2), LegacyEnchantmentRules.repairCost(primals, 10));
        assertTrue(LegacyEnchantmentRules.repairCost(primals, 0).isEmpty());
        assertEquals(Map.of("aer", 7, "ordo", 2), primals);
    }
}
