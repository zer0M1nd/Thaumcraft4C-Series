package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.research.ResearchTableBonuses.*;

class ResearchTableBonusesTest {
    private static final List<String> ASPECTS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    private static class World implements Environment {
        int y = 64;
        boolean dark;
        final List<String> visits = new ArrayList<>();
        @Override public int tableY() { return y; }
        @Override public int height() { return 256; }
        @Override public boolean darkCoveredNight() { return dark; }
        @Override public Source at(int x, int y, int z) { visits.add(x + "," + z + "," + y); return Source.NONE; }
    }
    @Test void intervalIsExactly600Ticks() {
        int counter = 0;
        for (int tick = 1; tick <= 599; tick++) { counter = ResearchTableBonuses.nextCounter(counter); assertEquals(tick, counter); }
        assertEquals(0, ResearchTableBonuses.nextCounter(counter));
    }
    @Test void scanOrderIncludesMinimumBuildHeightAndExcludesMaximum() {
        var world = new World(); world.y = 1;
        ResearchTableBonuses.recalculate(Set.of(), world, ASPECTS, bound -> fail("Unexpected random draw"));
        assertEquals(17 * 17 * 10, world.visits.size());
        assertEquals(List.of("-8,-8,-1", "-8,-8,0"), world.visits.subList(0, 2));
        assertEquals("-8,-7,-1", world.visits.get(10));
        assertEquals("8,8,8", world.visits.get(world.visits.size() - 1));
        var upper = new World(); upper.y = 255;
        ResearchTableBonuses.recalculate(Set.of(), upper, ASPECTS, bound -> bound - 1);
        assertEquals(17 * 17 * 9, upper.visits.size());
        assertEquals("8,8,0", upper.visits.get(upper.visits.size() - 1));
    }
    @Test void knownAirSkipsAllAltitudeDrawsWhileNightStillDraws() {
        var world = new World(); world.y = 200; world.dark = true;
        var draws = new ArrayList<Integer>();
        var original = Set.of("aer", "perditio");
        assertEquals(original, ResearchTableBonuses.recalculate(original, world, ASPECTS, bound -> { draws.add(bound); return 0; }));
        assertEquals(List.of(20), draws);
        assertEquals(4913, world.visits.size());
    }
    @Test void strictAltitudeThresholdDoesNotDrawAtEquality() {
        var world = new World(); world.y = 128;
        ResearchTableBonuses.recalculate(Set.of(), world, ASPECTS, bound -> fail("Must be strictly above half height"));
    }
    @Test void negativeBuildHeightIsScannedAndTheUpperBoundStaysExclusive() {
        var world = new World() {
            @Override public int minimumHeight() { return -64; }
            @Override public int height() { return 384; }
        };
        world.y = -63;
        ResearchTableBonuses.recalculate(Set.of(), world, ASPECTS, bound -> fail("Unexpected random draw"));
        assertEquals(17 * 17 * 10, world.visits.size());
        assertEquals("-8,-8,-1", world.visits.get(0));
        world.visits.clear(); world.y = 319;
        ResearchTableBonuses.recalculate(Set.of("aer"), world, ASPECTS, bound -> fail("Known air must not draw"));
        assertEquals(17 * 17 * 9, world.visits.size());
        assertEquals("8,8,0", world.visits.get(world.visits.size() - 1));
    }
    @Test void existingPrimalSkipsItsDrawAndFirstNewSuccessEndsScan() {
        var world = new World() {
            @Override public Source at(int x, int y, int z) { super.at(x, y, z); return Source.primal(y == -8 ? "aer" : "aqua", 15); }
        };
        var draws = new ArrayList<Integer>();
        assertEquals(Set.of("aer", "aqua"), ResearchTableBonuses.recalculate(Set.of("aer"), world, ASPECTS,
                bound -> { draws.add(bound); return 0; }));
        assertEquals(List.of(15), draws);
        assertEquals(2, world.visits.size());
    }
    @Test void bookshelfSuccessSelectsOnlyAMissingAspect() {
        var world = new World() { @Override public Source at(int x, int y, int z) { super.at(x, y, z); return Source.bookshelf(); } };
        var draws = new ArrayList<Integer>();
        assertEquals(Set.of("terra", "ignis"), ResearchTableBonuses.recalculate(Set.of("terra"), world, ASPECTS,
                bound -> { draws.add(bound); return bound == 300 ? 0 : 1; }));
        assertEquals(List.of(300, 5), draws);
        assertEquals(1, world.visits.size());
    }
    @Test void failedBookshelfAndBrainJarDrawsDoNotChooseAnAspect() {
        var world = new World() { @Override public Source at(int x, int y, int z) { super.at(x, y, z); return visits.size() == 1 ? Source.bookshelf() : Source.brainJar(); } };
        var draws = new ArrayList<Integer>();
        assertEquals(Set.of("perditio"), ResearchTableBonuses.recalculate(Set.of(), world, ASPECTS,
                bound -> { draws.add(bound); return bound == 300 ? 1 : bound == 200 ? 0 : 5; }));
        assertEquals(List.of(300, 200, 6), draws);
        assertEquals(2, world.visits.size());
    }
    @Test void invalidRandomCannotMutateOriginalSet() {
        var world = new World(); world.dark = true;
        var original = new java.util.LinkedHashSet<>(Set.of("terra"));
        assertThrows(IllegalArgumentException.class, () -> ResearchTableBonuses.recalculate(original, world, ASPECTS, bound -> bound));
        assertEquals(Set.of("terra"), original);
    }
}
