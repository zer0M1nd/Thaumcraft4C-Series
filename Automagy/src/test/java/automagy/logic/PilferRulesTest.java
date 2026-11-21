package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PilferRulesTest {
    @Test void threeStockModesUseDifferentQuantities() {
        var limit = PilferRules.Limit.unlimited();
        assertEquals(30, PilferRules.amount(1, .5, 64, 80, 20, 100, limit));
        assertEquals(16, PilferRules.amount(2, .5, 64, 80, 20, 100, limit));
        assertEquals(44, PilferRules.amount(3, .5, 64, 80, 20, 100, limit));
    }
    @Test void filterThresholdOverridesOnlyCorrespondingMode() {
        var limit = new PilferRules.Limit(7, 3);
        assertEquals(7, PilferRules.amount(3, .5, 0, 80, 20, 100, limit));
        assertEquals(0, PilferRules.amount(2, .5, 100, 80, 20, 100, limit));
        assertEquals(2, PilferRules.amount(3, .5, 64, 80, 20, 2, limit));
    }
    @Test void repeatedSingleItemsAccumulateHalfShares() {
        var first = PilferRules.share(1, 1, new double[]{.5}, new int[]{64}, new double[]{0});
        assertArrayEquals(new int[]{0}, first.amounts());
        var second = PilferRules.share(1, 2, new double[]{.5}, new int[]{64}, first.remainders());
        assertArrayEquals(new int[]{1}, second.amounts());
    }
    @Test void overSubscribedCollectorsNormalizeAndNeverOverdraw() {
        var share = PilferRules.share(100, 100, new double[]{1, 1}, new int[]{100, 100}, new double[]{0, 0});
        assertArrayEquals(new int[]{50, 50}, share.amounts());
        var rounded = PilferRules.share(1, 1, new double[]{.5, .5}, new int[]{10, 10}, new double[]{.9, .9});
        assertEquals(1, java.util.Arrays.stream(rounded.amounts()).sum());
    }
    @Test void fullCollectorsDoNotAccumulateDebtAndInvalidInputsAreBounded() {
        var share = PilferRules.share(100, 100, new double[]{.5}, new int[]{0}, new double[]{.2});
        assertArrayEquals(new int[]{0}, share.amounts()); assertEquals(.2, share.remainders()[0]);
        assertEquals(0, PilferRules.amount(2, .5, 64, 0, 0, 64, PilferRules.Limit.unlimited()));
        assertEquals(.5, PilferRules.boundedFraction(Double.NaN));
    }
}
