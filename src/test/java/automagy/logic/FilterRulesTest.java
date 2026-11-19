package automagy.logic;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FilterRulesTest {
    @Test void wildcardIsCaseInsensitiveAndTreatsRegexAsLiteral() {
        assertTrue(FilterRules.nameMatches(" *Iron* ", "Block of IRON"));
        assertTrue(FilterRules.nameMatches("[A]+*", "[a]+ ingot"));
        assertFalse(FilterRules.nameMatches("[A]+", "AAA"));
        assertFalse(FilterRules.nameMatches("iron", "iron nugget"));
        assertTrue(FilterRules.nameMatches("**a*b*", "xxaxxbxx"));
    }
    @Test void emptyFilterNeverPowersTally() {
        assertFalse(FilterRules.tally(List.of(), true, false, true, false, false));
    }
    @Test void blacklistInvertsAggregateRatherThanEveryAllCondition() {
        var counts = List.of(new FilterRules.Count(4, 4), new FilterRules.Count(0, 1));
        assertFalse(FilterRules.tally(counts, false, true, true, false, false));
        assertTrue(FilterRules.tally(counts, true, true, true, false, false));
        assertTrue(FilterRules.tally(counts, false, true, false, false, false));
        assertFalse(FilterRules.tally(counts, true, true, false, false, false));
    }
    @Test void countBoundaryAndLegacyNameShortCircuit() {
        var counts = List.of(new FilterRules.Count(999, 999));
        assertTrue(FilterRules.tally(counts, false, true, false, true, false));
        assertFalse(FilterRules.tally(counts, false, true, true, true, false));
        assertTrue(FilterRules.tally(List.of(), false, false, true, true, true));
        assertTrue(FilterRules.tally(List.of(new FilterRules.Count(Integer.MAX_VALUE, 999)), false, true, true, false, false));
    }
}
