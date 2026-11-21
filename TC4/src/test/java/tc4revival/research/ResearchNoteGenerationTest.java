package tc4revival.research;

import java.time.Duration;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteGenerationTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchNotes notes = new ResearchNotes(aspects);

    private ResearchDefinition definition(Map<String, Integer> outer, List<String> inner, List<String> negative, int complexity) {
        return new ResearchDefinition("TEST", "TEST", outer, List.of(), List.of(), List.of(), Set.of(), complexity, 0,
                new ResearchDefinition.Display(0, 0, Optional.empty(), Optional.empty(), 0),
                new ResearchDefinition.Triggers(Optional.empty(), Optional.empty(), Optional.empty()), List.of(),
                new ResearchDefinition.NoteSeeds(inner, negative));
    }

    @Test void singlePositiveAnchorGetsAnExtraCopyAndNegativesDoNotCountAsEndpoints() {
        for (boolean randomize : List.of(false, true)) for (var negatives : List.of(List.<String>of(), List.of("aqua", "perditio"))) {
            var def = definition(Map.of("terra", 3), List.of(), negatives, 1);
            var note = notes.generate(def, new Random(42)::nextInt, randomize).orElseThrow();
            var anchors = note.grid().entrySet().stream().filter(e -> e.getValue().anchor()).toList();
            assertEquals(2, anchors.size());
            for (var entry : anchors) {
                assertEquals("terra", entry.getValue().aspect());
                assertEquals(2, entry.getKey().radius());
            }
            assertEquals(negatives.size(), note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
            assertEquals(Map.of("terra", 3), def.aspects(), "Do not mutate the catalog");
            assertTrue(notes.complete(note, tag -> true).isEmpty(), "A new two-anchor note must not auto-complete");
            var nbt = new CompoundTag(); ResearchNoteNbt.write(nbt, note);
            assertEquals(note, ResearchNoteNbt.read(nbt, aspects).note().orElseThrow());
        }
    }

    @Test void aLoneInnerAnchorIsPreservedAndTwoExistingPositiveAnchorsAreNotDuplicated() {
        var innerOnly = notes.generate(definition(Map.of(), List.of("terra"), List.of("aqua"), 1), new Random(3)::nextInt).orElseThrow();
        assertEquals(2, innerOnly.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        assertEquals(1, innerOnly.grid().entrySet().stream().filter(e -> e.getValue().anchor() && e.getKey().radius() < 2).count());
        assertTrue(innerOnly.grid().values().stream().filter(ResearchNote.Cell::anchor).allMatch(c -> c.aspect().equals("terra")));
        var mixed = notes.generate(definition(Map.of("terra", 1), List.of("ordo"), List.of("aqua"), 1), new Random(3)::nextInt).orElseThrow();
        assertEquals(2, mixed.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        assertTrue(notes.generate(definition(Map.of(), List.of(), List.of("aqua"), 1), bound -> 0).isEmpty());
    }

    @Test void allPositiveSeedsRemainConnectedWithoutTraversingNegativesAfterCarving() {
        for (int complexity = 2; complexity <= 3; complexity++) for (boolean randomize : List.of(false, true)) {
            var def = definition(Map.of("terra", 1, "ordo", 1), List.of("aer", "ignis", "sensus"), List.of("aqua", "perditio", "alienis"), complexity);
            for (int seed = 0; seed < 300; seed++) {
                var note = notes.generate(def, new Random(seed)::nextInt, randomize).orElseThrow();
                assertEquals(5, note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
                assertEquals(3, note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
                assertConnectedAnchors(note.grid());
                int radius = complexity + 1, full = 1 + 3 * radius * (radius + 1);
                assertEquals(full - complexity * 2, note.grid().size(), "The requested holes fit outside this small subtree");
                assertEquals(note, notes.generate(def, new Random(seed)::nextInt, randomize).orElseThrow());
            }
        }
    }

    @Test void treeAvoidsNegativeShortcutAndOversizedBudgetStopsAtProtectedAndFixedCells() {
        var grid = disk(2);
        var left = new ResearchHex(-2, 0); var right = new ResearchHex(2, 0); var negative = new ResearchHex(0, 0);
        grid.put(left, new ResearchNote.Cell(1, "terra")); grid.put(right, new ResearchNote.Cell(1, "ordo"));
        grid.put(negative, new ResearchNote.Cell(3, "aqua"));
        var before = new LinkedHashMap<>(grid);
        var tree = ResearchNoteHoles.connectingSubtree(grid, new Random(7)::nextInt).orElseThrow();
        assertEquals(before, grid, "Planning must not carve or move seeds");
        assertFalse(tree.contains(negative)); assertTrue(tree.containsAll(List.of(left, right)));
        assertEquals(6, tree.size(), "Two endpoints use a shortest path around the forbidden center");
        ResearchNoteHoles.carve(grid, 1000, new Random(7)::nextInt);
        var retained = new HashSet<>(tree); retained.add(negative);
        assertEquals(retained, grid.keySet());
        assertEquals(before.get(negative), grid.get(negative));
        assertConnectedAnchors(grid);
    }

    @Test void compactSubtreePrunesUnneededBranchesAndStillVariesWithTheSeed() {
        var grid = disk(2);
        for (var hex : List.of(new ResearchHex(0, 0), new ResearchHex(1, 0), new ResearchHex(0, 1))) grid.put(hex, new ResearchNote.Cell(1, "terra"));
        assertEquals(3, ResearchNoteHoles.connectingSubtree(grid, new Random(9)::nextInt).orElseThrow().size());
        grid = disk(3);
        grid.put(new ResearchHex(-3, 0), new ResearchNote.Cell(1, "terra"));
        grid.put(new ResearchHex(2, 1), new ResearchNote.Cell(1, "ordo"));
        var layouts = new HashSet<Set<ResearchHex>>();
        for (int seed = 0; seed < 50; seed++) layouts.add(ResearchNoteHoles.connectingSubtree(grid, new Random(seed)::nextInt).orElseThrow());
        assertTrue(layouts.size() > 1, "Random trees must permit different equally short paths");
    }

    @Test void disconnectedAuthoredAnchorsDoNotLoseMoreCellsAndZeroBudgetUsesNoRandomness() {
        var grid = disk(2); var center = new ResearchHex(0, 0);
        grid.put(center, new ResearchNote.Cell(1, "terra")); grid.put(new ResearchHex(2, 0), new ResearchNote.Cell(1, "ordo"));
        for (var hex : ResearchHex.ring(1)) grid.put(hex, new ResearchNote.Cell(3, "aqua"));
        var before = new LinkedHashMap<>(grid);
        assertTrue(ResearchNoteHoles.connectingSubtree(grid, bound -> 0).isEmpty());
        ResearchNoteHoles.carve(grid, 1000, bound -> 0); assertEquals(before, grid);
        ResearchNoteHoles.carve(grid, 0, bound -> { fail("No random draws without holes"); return 0; });
        assertEquals(before, grid);
    }

    @Test void boundedGenerationCostOnLargestBoardsIncludingConstantRandomSources() {
        var def = definition(Map.of("terra", 1, "ordo", 1), List.of("aer", "ignis"), List.of("aqua", "perditio"), 3);
        assertTimeout(Duration.ofSeconds(10), () -> {
            for (int i = 0; i < 500; i++) notes.generate(def, new Random(i)::nextInt);
            long[] nanos = new long[3000];
            for (int i = 0; i < nanos.length; i++) {
                var random = new Random(i); long start = System.nanoTime();
                var note = notes.generate(def, i % 2 == 0 ? random::nextInt : bound -> 0).orElseThrow();
                nanos[i] = System.nanoTime() - start;
                assertConnectedAnchors(note.grid());
            }
            Arrays.sort(nanos);
            System.out.printf(Locale.ROOT, "RESEARCH_NOTE_BENCHMARK n=%d mean_us=%.3f p50_us=%.3f p95_us=%.3f p99_us=%.3f max_us=%.3f%n",
                    nanos.length, Arrays.stream(nanos).average().orElseThrow() / 1000, nanos[1500] / 1000.0,
                    nanos[2850] / 1000.0, nanos[2970] / 1000.0, nanos[2999] / 1000.0);
        });
    }

    private LinkedHashMap<ResearchHex, ResearchNote.Cell> disk(int radius) {
        var grid = new LinkedHashMap<ResearchHex, ResearchNote.Cell>(); grid.put(new ResearchHex(0, 0), ResearchNote.Cell.blank());
        for (int r = 1; r <= radius; r++) for (var hex : ResearchHex.ring(r)) grid.put(hex, ResearchNote.Cell.blank());
        return grid;
    }

    private void assertConnectedAnchors(Map<ResearchHex, ResearchNote.Cell> grid) {
        var anchors = grid.entrySet().stream().filter(e -> e.getValue().anchor()).map(Map.Entry::getKey).toList();
        var reached = new HashSet<ResearchHex>(); var pending = new ArrayDeque<ResearchHex>(); pending.add(anchors.get(0));
        while (!pending.isEmpty()) {
            var current = pending.removeFirst(); if (!reached.add(current)) continue;
            for (int side = 0; side < 6; side++) {
                var next = current.neighbor(side); var cell = grid.get(next);
                if (cell != null && !cell.negative() && !reached.contains(next)) pending.addLast(next);
            }
        }
        assertTrue(reached.containsAll(anchors), "All positive anchors need a path avoiding negative cells");
    }
}
