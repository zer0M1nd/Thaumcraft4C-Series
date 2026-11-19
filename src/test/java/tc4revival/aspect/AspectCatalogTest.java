package tc4revival.aspect;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectCatalogTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());

    @Test void originalCatalogHasSixPrimalsAndFortyTwoCompounds() {
        assertEquals(48, catalog.all().size());
        assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"),
                catalog.primals().stream().map(Aspect::tag).toList());
        assertEquals(42, catalog.compounds().size());
        assertEquals(771, catalog.require("perditio").blendFactor());
        assertEquals(771, catalog.require("vacuos").blendFactor());
        assertEquals(1, catalog.require("vitium").blendFactor());
        assertEquals("Metallum", catalog.require("metallum").displayName());
    }

    @Test void everyCompoundCanBeDiscoveredInEitherIngredientOrder() {
        for (Aspect compound : catalog.compounds()) {
            var first = catalog.require(compound.components().get(0));
            var second = catalog.require(compound.components().get(1));
            assertEquals(compound, catalog.combine(first, second).orElseThrow());
            assertEquals(compound, catalog.combine(second, first).orElseThrow());
        }
        assertTrue(catalog.combine(catalog.require("aer"), catalog.require("terra")).isEmpty());
        assertTrue(catalog.combine(catalog.require("aer"), null).isEmpty());
    }

    @Test void metalExpandsBothEarthBranchesAndRetainsDiscoveryOrder() {
        var result = catalog.toPrimals(new AspectAmounts().add(catalog.require("metallum"), 4), AspectCatalog.Reduction.SUM);
        assertEquals(8, result.amount(catalog.require("terra")));
        assertEquals(4, result.amount(catalog.require("ordo")));
        assertEquals(12, result.total());
        assertEquals(List.of("terra", "ordo"), result.aspects().stream().map(Aspect::tag).toList());
    }

    @Test void humanHasTenPrimalUnitsButMaximumModeHasSix() {
        var input = new AspectAmounts().add(catalog.require("humanus"), 1);
        var sum = catalog.toPrimals(input, AspectCatalog.Reduction.SUM);
        assertEquals(10, sum.total());
        assertEquals(3, sum.amount(catalog.require("aqua")));
        assertEquals(3, sum.amount(catalog.require("terra")));
        for (String single : List.of("aer", "ordo", "ignis", "perditio")) {
            assertEquals(1, sum.amount(catalog.require(single)));
        }
        var maximum = catalog.toPrimals(input, AspectCatalog.Reduction.MAXIMUM);
        assertEquals(6, maximum.total());
        assertTrue(maximum.view().values().stream().allMatch(value -> value == 1));
        assertEquals(1, input.total(), "Decomposition must not consume the source");
    }

    @Test void negativeAndZeroQuantitiesRetainOldSignedRules() {
        var input = new AspectAmounts().add(catalog.require("metallum"), -2).add(catalog.require("lux"), 0);
        var result = catalog.toPrimals(input, AspectCatalog.Reduction.MAXIMUM);
        assertEquals(-2, result.amount(catalog.require("terra")));
        assertEquals(-2, result.amount(catalog.require("ordo")));
        assertTrue(result.contains(catalog.require("ignis")));
    }

    @Test void repeatedImmediateComponentsAreAddedBeforeMaximumReduction() {
        var graph = new AspectCatalog(List.of(
                new Aspect("a", 0, List.of(), "", 1),
                new Aspect("b", 0, List.of("a", "a"), "", 1)));
        var result = graph.toPrimals(new AspectAmounts().add(graph.require("b"), 3), AspectCatalog.Reduction.MAXIMUM);
        assertEquals(6, result.amount(graph.require("a")));
    }

    @Test void invalidGraphsFailBeforeTheyCanCauseUnboundedDecomposition() {
        var air = catalog.require("aer");
        assertThrows(IllegalArgumentException.class, () -> new AspectCatalog(List.of(air, air)));
        assertThrows(IllegalArgumentException.class, () -> new AspectCatalog(List.of(
                new Aspect("a", 0, List.of("b", "missing"), "", 1))));
        assertThrows(IllegalArgumentException.class, () -> new AspectCatalog(List.of(air,
                new Aspect("a", 0, List.of("b", "aer"), "", 1),
                new Aspect("b", 0, List.of("a", "aer"), "", 1))));
    }

    @Test void everyBuiltInTerminatesInPrimals() {
        for (Aspect aspect : catalog.all()) {
            var result = catalog.toPrimals(new AspectAmounts().add(aspect, 1), AspectCatalog.Reduction.SUM);
            assertFalse(result.isEmpty());
            assertTrue(result.aspects().stream().allMatch(Aspect::isPrimal));
        }
    }
}
