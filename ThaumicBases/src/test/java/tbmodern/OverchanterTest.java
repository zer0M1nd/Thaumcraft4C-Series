package tbmodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OverchanterTest {
    @Test void fullChargeUsesThirtyLegacyLevels() { assertEquals(30, Overchanter.legacyLevelsForExperience(825)); }
    @Test void partialContainersReduceThePlayerCharge() {
        assertEquals(0, Overchanter.legacyLevelsForExperience(0));
        assertEquals(1, Overchanter.legacyLevelsForExperience(17));
        assertEquals(15, Overchanter.legacyLevelsForExperience(255));
        assertEquals(15, Overchanter.legacyLevelsForExperience(256));
        assertEquals(16, Overchanter.legacyLevelsForExperience(272));
    }
}
