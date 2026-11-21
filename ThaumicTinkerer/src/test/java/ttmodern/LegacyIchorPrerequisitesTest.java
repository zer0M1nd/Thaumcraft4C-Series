package ttmodern;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class LegacyIchorPrerequisitesTest {
    @Test void originalAuthorsOnlyAndNoIchorDescendants() {
        var keys=LegacyIchorPrerequisites.KEYS;
        assertEquals(keys.size(),Set.copyOf(keys).size());
        assertTrue(keys.containsAll(Set.of("CAP_silver","PURESILVER","THAUMIUM","FOCUSHELLBAT",
                "thaumic_tinkerer:ENCHANTER","thaumic_tinkerer:DIMENSION_SHARDS","thaumic_tinkerer:TTENCH_FINAL")));
        for(String key:Set.of("ELDRITCHMINOR","PRIMPEARL","REVIVAL_RESEARCH_ASSISTANCE",
                "thaumic_tinkerer:ICHOR","thaumic_tinkerer:ICHORIUM","thaumic_tinkerer:INFUSED_INKWELL"))
            assertFalse(keys.contains(key),key);
    }
}
