package tc4revival.content;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacySwordRulesTest {
    @Test void blockingRetainsOldSmallDamageQuirkAndArmorBypass() {
        assertEquals(5.5f, LegacySwordRules.blockedDamage(10, false));
        assertEquals(.75f, LegacySwordRules.blockedDamage(.5f, false));
        assertEquals(1f, LegacySwordRules.blockedDamage(1, false));
        assertEquals(10f, LegacySwordRules.blockedDamage(10, true));
        assertEquals(0f, LegacySwordRules.blockedDamage(0, false));
        assertEquals(-1f, LegacySwordRules.blockedDamage(-1, false));
    }
    @Test void thaumiumDamageModifierAndWeaponAspectUseDifferentLegacyFormulas() {
        assertEquals(6f, LegacySwordRules.attackModifier(2));
        assertEquals(3, LegacySwordRules.weaponAspect(2));
        assertEquals(1, LegacySwordRules.hitWear());
    }
    @Test void fastPlantMiningDoesNotGrantCobwebHarvestPermission() {
        assertEquals(15f, LegacySwordRules.destroySpeed(LegacySwordRules.Surface.COBWEB));
        assertTrue(LegacySwordRules.harvests(LegacySwordRules.Surface.COBWEB));
        for (var surface : LegacySwordRules.Surface.values()) {
            if (surface == LegacySwordRules.Surface.COBWEB) continue;
            assertFalse(LegacySwordRules.harvests(surface));
            assertEquals(surface == LegacySwordRules.Surface.OTHER ? 1f : 1.5f, LegacySwordRules.destroySpeed(surface));
        }
    }
    @Test void miningWearDependsOnNonzeroHardnessRatherThanHarvestSuccess() {
        assertEquals(0, LegacySwordRules.miningWear(0));
        assertEquals(0, LegacySwordRules.miningWear(-0f));
        assertEquals(2, LegacySwordRules.miningWear(.1f));
        assertEquals(2, LegacySwordRules.miningWear(50f));
        assertEquals(2, LegacySwordRules.miningWear(-1f));
    }
}
