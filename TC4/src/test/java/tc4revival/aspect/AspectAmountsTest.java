package tc4revival.aspect;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectAmountsTest {
    private final Aspect air = LegacyAspect.AIR.create();
    private final Aspect earth = LegacyAspect.EARTH.create();
    private final Aspect fire = LegacyAspect.FIRE.create();
    private final Aspect water = LegacyAspect.WATER.create();
    private final Aspect order = LegacyAspect.ORDER.create();

    @Test void mergeIsMaximumWhileAddIsSumAndOrderingStaysStable() {
        var amounts = new AspectAmounts().add(earth, 3).add(air, 2).merge(earth, 2).merge(air, 7).add(earth, 1);
        assertEquals(4, amounts.amount(earth));
        assertEquals(7, amounts.amount(air));
        assertEquals(List.of(earth, air), amounts.aspects());
        assertEquals(List.of(air, earth), amounts.alphabetical());
    }

    @Test void drainedNodeRetainsZeroButRemovingTheSameQuantityDiscardsEntry() {
        var amounts = new AspectAmounts().add(air, 3);
        assertFalse(amounts.reduce(air, 4));
        assertEquals(3, amounts.amount(air));
        assertTrue(amounts.reduce(air, 3));
        assertTrue(amounts.contains(air));
        assertEquals(0, amounts.total());
        amounts.remove(air, 0);
        assertFalse(amounts.contains(air));
        assertTrue(amounts.reduce(air, 0), "Old reduction by zero creates the entry");
        assertTrue(amounts.contains(air));
    }

    @Test void signedArithmeticIsPreservedForInternalCallers() {
        var amounts = new AspectAmounts().add(air, -3).merge(air, -2);
        assertEquals(-2, amounts.amount(air));
        assertTrue(amounts.reduce(air, -4));
        assertEquals(2, amounts.amount(air));
        amounts.remove(earth, -2);
        assertEquals(2, amounts.amount(earth));
    }

    @Test void sortingPlacesNonpositiveAmountsLastAndKeepsStableTies() {
        var amounts = new AspectAmounts().add(air, 1).add(earth, 3).add(fire, 0).add(water, 2).add(order, 2);
        assertEquals(List.of(earth, water, order, air, fire), amounts.byAmount());
        amounts.add(fire, -1);
        assertEquals(List.of(earth, water, order, air, fire), amounts.byAmount());
    }

    @Test void emptyAndCopiedListsDoNotExposeNullsOrMutableBackingData() {
        var empty = new AspectAmounts();
        assertTrue(empty.aspects().isEmpty());
        assertTrue(empty.byAmount().isEmpty());
        assertTrue(empty.primals().isEmpty());
        var original = new AspectAmounts().add(air, 3);
        var copy = original.copy().add(air, 2);
        assertEquals(3, original.total());
        assertEquals(5, copy.total());
        assertThrows(UnsupportedOperationException.class, () -> original.view().put(air, 8));
    }

    @Test void overflowDoesNotWrapOrLeaveHalfAppliedTransfers() {
        var amounts = new AspectAmounts().add(air, Integer.MAX_VALUE).add(earth, 2);
        assertEquals(2147483649L, amounts.total());
        assertThrows(ArithmeticException.class, () -> amounts.add(air, 1));
        var incoming = new AspectAmounts().add(earth, 3).add(air, 1);
        assertThrows(ArithmeticException.class, () -> amounts.add(incoming));
        assertEquals(Integer.MAX_VALUE, amounts.amount(air));
        assertEquals(2, amounts.amount(earth));
    }

    @Test void combiningWithSelfUsesOriginalQuantities() {
        var amounts = new AspectAmounts().add(air, 3).add(earth, 5);
        amounts.add(amounts);
        assertEquals(6, amounts.amount(air));
        assertEquals(10, amounts.amount(earth));
        amounts.merge(amounts);
        assertEquals(16, amounts.total());
    }
}
