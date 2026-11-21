package nodalmechanics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;

class NodalRulesTest {
    private static final Set<String> KNOWN = Set.of("aer", "ignis", "addon:custom");
    @Test void eightPhialsProduce120VisFor1200Essentia() {
        var counts = NodalRules.counts("aer,aer,aer,aer,aer,aer,aer,aer", KNOWN::contains);
        assertEquals(Map.of("aer", 8), counts);
        assertEquals(120, counts.get("aer") * NodalRules.CAPACITY_PER_PHIAL);
        assertEquals(1200, counts.get("aer") * NodalRules.ESSENTIA_PER_PHIAL);
    }
    @Test void mixturesAndAddonAspectsAreNotReducedToPrimals() {
        assertEquals(Map.of("aer", 2, "ignis", 1, "addon:custom", 1), NodalRules.counts("aer,ignis,aer,addon:custom", KNOWN::contains));
    }
    @Test void malformedOrOversizedTagsFailClosed() {
        for (String input : new String[]{"", "unknown", "aer,", ",aer", "aer,,ignis", "aer,aer,aer,aer,aer,aer,aer,aer,aer"})
            assertTrue(NodalRules.counts(input, KNOWN::contains).isEmpty(), input);
    }
    @Test void originalAuthorTypeDistributionIsExact() {
        int[] counts = new int[6];
        for (int roll = 0; roll < 100; roll++) counts[NodalRules.type(roll)]++;
        assertArrayEquals(new int[]{80, 5, 3, 3, 5, 4}, counts);
    }
    @Test void originalAuthorModifierDistributionIsExact() {
        int[] counts = new int[4];
        for (int presence = 0; presence < 100; presence++) for (int roll = 0; roll < 50; roll++) counts[NodalRules.modifier(presence, roll) + 1]++;
        assertArrayEquals(new int[]{2000, 1200, 1200, 600}, counts);
    }
}
