package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNotesTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchNotes notes = new ResearchNotes(aspects);
    private JsonArray fixture(String file) {
        try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass().getResourceAsStream("/" + file)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private ResearchCatalog catalog() { return ResearchCatalog.decode(fixture("research-catalog.json"), aspects); }
    private ResearchNote note(Map<ResearchHex, ResearchNote.Cell> grid) { return new ResearchNote("THAUMIUM", 123, false, 2, grid); }
    private ResearchNote.Cell anchor(String aspect) { return new ResearchNote.Cell(1, aspect); }
    private ResearchNote.Cell placed(String aspect) { return new ResearchNote.Cell(2, aspect); }

    @Test void all648RecordedInputsPreserveAnchorsColorAndNbtWithConnectedDeterministicAnchors() {
        var evidence = fixture("research-notes.json");
        assertEquals(648, evidence.size());
        var catalog = catalog();
        for (var element : evidence) {
            var row = element.getAsJsonObject();
            String key = row.get("key").getAsString();
            long seed = row.get("seed").getAsLong();
            String label = key + " seed=" + seed;
            var random = new Random(seed);
            var actual = notes.generate(catalog.require(key), random::nextInt).orElseThrow();
            var expected = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
            for (var value : row.getAsJsonArray("grid")) {
                var cell = value.getAsJsonObject();
                expected.put(new ResearchHex(cell.get("q").getAsInt(), cell.get("r").getAsInt()),
                        new ResearchNote.Cell(cell.get("type").getAsInt(), cell.has("aspect") ? cell.get("aspect").getAsString() : null));
            }
            // The recorded oracle still defines the research seeds and colour. Modern
            // generation deliberately protects the anchors' connecting subtree before carving.
            assertEquals(expected.values().stream().filter(c -> c.type() == 1).map(ResearchNote.Cell::aspect).sorted().toList(),
                    actual.grid().values().stream().filter(c -> c.type() == 1).map(ResearchNote.Cell::aspect).sorted().toList(), label + " anchors");
            int radius = Math.max(1, catalog.require(key).complexity()) + 1;
            int fullSize = 1 + 3 * radius * (radius + 1);
            int maxHoles = catalog.require(key).complexity() > 1 ? 2 * catalog.require(key).complexity() : 0;
            assertTrue(actual.grid().size() >= fullSize - maxHoles && actual.grid().size() <= fullSize, label + " hole budget");
            assertTrue(actual.grid().keySet().stream().allMatch(h -> h.radius() <= radius), label + " bounds");
            var ordered = new java.util.ArrayList<ResearchHex>(); ordered.add(new ResearchHex(0, 0));
            for (int r = 1; r <= radius; r++) ordered.addAll(ResearchHex.ring(r));
            ordered.removeIf(h -> !actual.grid().containsKey(h));
            assertEquals(ordered, List.copyOf(actual.grid().keySet()), label + " stable order");
            var remaining = new java.util.HashSet<>(actual.grid().keySet());
            var queue = new java.util.ArrayDeque<ResearchHex>(); queue.add(actual.grid().entrySet().stream()
                    .filter(e -> e.getValue().anchor()).map(Map.Entry::getKey).findFirst().orElseThrow());
            while (!queue.isEmpty()) {
                var hex = queue.removeFirst(); if (!remaining.remove(hex)) continue;
                for (int side = 0; side < 6; side++) if (remaining.contains(hex.neighbor(side))) queue.add(hex.neighbor(side));
            }
            assertTrue(actual.grid().entrySet().stream().filter(e -> e.getValue().anchor())
                    .noneMatch(e -> remaining.contains(e.getKey())), label + " connected anchors");
            var repeated = notes.generate(catalog.require(key), new Random(seed)::nextInt).orElseThrow();
            assertEquals(actual, repeated, label + " reproducible seed");
            assertEquals(row.get("color").getAsInt(), actual.color(), label);
            assertFalse(actual.complete(), label);
            assertEquals(0, actual.copies(), label);
            var nbt = new CompoundTag();
            nbt.putString("unrelated", "retained");
            ResearchNoteNbt.write(nbt, actual);
            var read = ResearchNoteNbt.read(nbt, aspects);
            assertTrue(read.problems().isEmpty(), label);
            assertEquals(actual, read.note().orElseThrow(), label + " NBT");
            assertEquals("retained", nbt.getString("unrelated"));
        }
    }

    @Test void ringsContainExactlySixSidesWithLegacyStartAndDirectionOrder() {
        assertEquals(List.of(new ResearchHex(-1, 1), new ResearchHex(0, 1), new ResearchHex(1, 0),
                new ResearchHex(1, -1), new ResearchHex(0, -1), new ResearchHex(-1, 0)), ResearchHex.ring(1));
        for (int radius = 1; radius <= 4; radius++) {
            var ring = ResearchHex.ring(radius);
            assertEquals(6 * radius, ring.size());
            assertEquals(ring.size(), new java.util.HashSet<>(ring).size());
            for (var hex : ring) assertEquals(radius, hex.radius());
        }
    }

    @Test void noPositivePrimaryProducesNoNoteAndDoesNotConsumeRandom() {
        var base = catalog().require("THAUMIUM");
        var zero = new ResearchDefinition(base.key(), base.category(), Map.of("terra", 0), base.parents(),
                base.hiddenParents(), base.siblings(), base.flags(), base.complexity(), base.warp(), base.display(), base.triggers());
        assertTrue(notes.generate(zero, bound -> { fail("Unexpected random draw"); return 0; }).isEmpty());
        var equal = new LinkedHashMap<String, Integer>();
        equal.put("ignis", 4); equal.put("aqua", 4);
        var tied = new ResearchDefinition(base.key(), base.category(), equal, base.parents(), base.hiddenParents(),
                base.siblings(), base.flags(), 0, base.warp(), base.display(), base.triggers());
        assertEquals(aspects.require("ignis").color(), notes.generate(tied, new Random(0)::nextInt).orElseThrow().color());
    }

    @Test void declaredZeroComplexityUsesTheOriginalMinimumBoard() {
        var base = catalog().require("THAUMIUM");
        var zero = new ResearchDefinition(base.key(), base.category(), base.aspects(), base.parents(),
                base.hiddenParents(), base.siblings(), base.flags(), 0, base.warp(), base.display(), base.triggers());
        var one = new ResearchDefinition(base.key(), base.category(), base.aspects(), base.parents(),
                base.hiddenParents(), base.siblings(), base.flags(), 1, base.warp(), base.display(), base.triggers());
        var generated = notes.generate(zero, new Random(42)::nextInt).orElseThrow();
        assertEquals(19, generated.grid().size());
        assertEquals(notes.generate(one, new Random(42)::nextInt).orElseThrow(), generated);
    }

    @Test void oldSevenCellNotesGainSpaceWithoutLosingProgressOrMetadata() {
        var cells = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        cells.put(new ResearchHex(0, 0), placed("aer"));
        ResearchHex.ring(1).forEach(hex -> cells.put(hex, ResearchNote.Cell.blank()));
        cells.put(new ResearchHex(0, 1), anchor("victus"));
        cells.put(new ResearchHex(1, 0), anchor("praecantatio"));
        cells.put(new ResearchHex(0, -1), anchor("metallum"));
        cells.put(new ResearchHex(-1, 0), anchor("permutatio"));
        var old = new ResearchNote("TB.Metalleat", 14548997, false, 3, cells);
        var expanded = ResearchNotes.expandUndersized(old);
        assertEquals(19, expanded.grid().size());
        cells.forEach((hex, cell) -> assertEquals(cell, expanded.grid().get(hex)));
        assertTrue(ResearchHex.ring(2).stream().allMatch(hex -> expanded.grid().get(hex).equals(ResearchNote.Cell.blank())));
        assertEquals(old.key(), expanded.key()); assertEquals(old.color(), expanded.color());
        assertEquals(old.copies(), expanded.copies()); assertFalse(expanded.complete());
        assertEquals(7, old.grid().size());
        assertSame(expanded, ResearchNotes.expandUndersized(expanded));
        var nbt = new CompoundTag(); nbt.putString("custom", "retained");
        ResearchNoteNbt.write(nbt, expanded);
        assertEquals(expanded, ResearchNoteNbt.read(nbt, aspects).note().orElseThrow());
        assertEquals("retained", nbt.getString("custom"));
        var completed = new ResearchNote(old.key(), old.color(), true, old.copies(), cells);
        assertSame(completed, ResearchNotes.expandUndersized(completed));
        var sparse = new ResearchNote(old.key(), old.color(), false, 0,
                Map.of(new ResearchHex(0, 2), anchor("alienis")));
        assertSame(sparse, ResearchNotes.expandUndersized(sparse));
    }

    @Test void onlyKnownDirectComponentsConnectAndFailureCannotModifyTheInput() {
        var source = note(Map.of(new ResearchHex(-1, 0), anchor("terra"), new ResearchHex(0, 0), placed("vitreus"),
                new ResearchHex(1, 0), anchor("ordo")));
        assertTrue(notes.complete(source, tag -> !tag.equals("vitreus")).isEmpty());
        assertFalse(source.complete());
        assertEquals(3, source.grid().size());
        assertTrue(notes.complete(source, tag -> true).orElseThrow().complete());
        for (String other : List.of("terra", "aer", "instrumentum")) {
            assertTrue(notes.complete(note(Map.of(new ResearchHex(0, 0), anchor("terra"),
                    new ResearchHex(1, 0), anchor(other))), tag -> true).isEmpty(), other);
        }
        assertTrue(notes.complete(note(Map.of(new ResearchHex(0, 0), anchor("vitreus"),
                new ResearchHex(1, 0), anchor("terra"))), tag -> true).isPresent());
    }

    @Test void completedGridRetainsConnectedBranchesButRemovesBlanksAndUnrelatedIslands() {
        var cells = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        cells.put(new ResearchHex(-1, 0), anchor("terra"));
        cells.put(new ResearchHex(0, 0), placed("vitreus"));
        cells.put(new ResearchHex(1, 0), anchor("ordo"));
        cells.put(new ResearchHex(0, 1), placed("terra")); // Connected branch, not needed to reach the last anchor.
        cells.put(new ResearchHex(2, 0), placed("ignis")); // Adjacent but unrelated.
        cells.put(new ResearchHex(-4, 0), placed("terra"));
        cells.put(new ResearchHex(-3, 0), ResearchNote.Cell.blank());
        var result = notes.complete(note(cells), tag -> true).orElseThrow();
        assertEquals(List.of(new ResearchHex(-1, 0), new ResearchHex(0, 0), new ResearchHex(1, 0), new ResearchHex(0, 1)),
                List.copyOf(result.grid().keySet()));
        assertEquals(2, result.copies());
        assertEquals(123, result.color());
        assertEquals(result, notes.complete(result, tag -> true).orElseThrow());
    }

    @Test void emptyNotesRejectAndSingleAnchorNotesRetainTheirAnchor() {
        assertTrue(notes.complete(note(Map.of(new ResearchHex(0, 0), placed("terra"))), tag -> false).isEmpty());
        assertTrue(notes.complete(note(Map.of()), tag -> true).isEmpty());
        var anchorOnly = note(Map.of(new ResearchHex(0, 0), anchor("terra"), new ResearchHex(1, 0), placed("vitreus")));
        assertEquals(Map.of(new ResearchHex(0, 0), anchor("terra")), notes.complete(anchorOnly, tag -> false).orElseThrow().grid());
    }

    @Test void noteCapturesItsGridAndPathologicalRandomStillTerminates() {
        var grid = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        grid.put(new ResearchHex(0, 0), anchor("terra"));
        var captured = note(grid);
        grid.clear();
        assertEquals(1, captured.grid().size());
        assertThrows(UnsupportedOperationException.class, () -> captured.grid().clear());
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            // A constant source selects legal candidates directly, without rejection loops.
            var generated = notes.generate(catalog().require("ROD_primal_staff"), bound -> { calls.incrementAndGet(); return 0; }).orElseThrow();
            assertEquals(55, generated.grid().size());
            assertTrue(calls.get() < 5000, "Generation must have bounded random draws, even with a constant source");
        });
    }

    @Test void damagedNbtRejectsTheWholeBoardAndDuplicateCoordinatesKeepLastCell() {
        var original = note(Map.of(new ResearchHex(0, 0), anchor("terra"), new ResearchHex(1, 0), anchor("vitreus")));
        var tag = new CompoundTag();
        ResearchNoteNbt.write(tag, original);
        var rows = (ListTag) tag.get("hexgrid");
        var duplicate = rows.getCompound(0).copy();
        duplicate.putString("aspect", "ordo");
        rows.add(duplicate);
        var duplicateHex = new ResearchHex(duplicate.getByte("hexq"), duplicate.getByte("hexr"));
        assertEquals("ordo", ResearchNoteNbt.read(tag, aspects).note().orElseThrow().grid().get(duplicateHex).aspect());
        for (int damage = 0; damage < 5; damage++) {
            var bad = tag.copy();
            var cell = ((ListTag) bad.get("hexgrid")).getCompound(0);
            switch (damage) {
                case 0 -> cell.putString("aspect", "missing");
                case 1 -> cell.remove("aspect");
                case 2 -> cell.putByte("type", (byte) 4);
                case 3 -> cell.putByte("hexq", (byte) 100);
                case 4 -> bad.putInt("copies", -1);
            }
            var result = ResearchNoteNbt.read(bad, aspects);
            assertTrue(result.note().isEmpty(), "damage=" + damage);
            assertFalse(result.problems().isEmpty());
        }
        assertTrue(ResearchNoteNbt.read(null, aspects).note().isEmpty());
        assertEquals(0, ResearchNoteNbt.read(new CompoundTag(), aspects).note().orElseThrow().grid().size());
    }
}
