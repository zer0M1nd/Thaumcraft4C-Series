package tc4revival.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.client.ResearchTableView;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteLayoutTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchNotes notes = new ResearchNotes(aspects);
    private static final ResearchHex CENTER = new ResearchHex(0, 0);
    private static final String MIXED = """
            {"size":2,"cells":[
              {"q":0,"r":0,"state":"empty"},
              {"q":-1,"r":0,"state":"initial","aspect":"terra"},
              {"q":1,"r":0,"state":"initial","aspect":"ordo"},
              {"q":0,"r":-1,"state":"negative","aspect":"ignis"},
              {"q":0,"r":1,"state":"blocked"}]}
            """;

    private JsonObject source(String layout) {
        var json = JsonParser.parseString("""
            {"categories":[{"key":"BASICS","icon":"test:icon","background":"test:book","research":[
              {"key":"test:layout","category":"BASICS","displayColumn":0,"displayRow":0,"pageCount":0,
               "complexity":3,"aspects":{"terra":1,"ordo":1}},
              {"key":"RESEARCHDUPE","category":"BASICS","displayColumn":1,"displayRow":0,"pageCount":0}]}],
             "pages":[{"key":"test:layout","pages":[]},{"key":"RESEARCHDUPE","pages":[]}],"recipes":[]}
            """).getAsJsonObject();
        if (layout != null) entry(json).add("noteLayout", JsonParser.parseString(layout));
        return json;
    }

    private JsonObject entry(JsonObject json) {
        return json.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject();
    }
    private ResearchDefinitions.Snapshot decode(String layout) { return ResearchDefinitions.decode(source(layout), aspects); }
    private ResearchNote generate(ResearchDefinition definition) {
        return notes.generate(definition, bound -> { throw new AssertionError("Exact layout consumed randomness"); }, true).orElseThrow();
    }
    private ResearchNote generate(String layout) { return generate(decode(layout).catalog().require("test:layout")); }
    private ResearchTableActions.State table(ResearchNote note) {
        return new ResearchTableActions.State(Optional.of(note), 1, 10, Set.of());
    }
    private ResearchNote persisted(ResearchNote note) throws Exception {
        var tag = new CompoundTag(); tag.putString("addon", "retained"); ResearchNoteNbt.write(tag, note);
        var bytes = new ByteArrayOutputStream(); NbtIo.writeCompressed(tag, bytes);
        var stored = NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals("retained", stored.getString("addon"));
        var result = ResearchNoteNbt.read(stored, aspects);
        assertTrue(result.problems().isEmpty());
        assertEquals(note, result.note().orElseThrow());
        return result.note().orElseThrow();
    }
    @AfterEach void resetOverride() { ResearchNoteTestSeeds.set(0, 0); }

    @Test void fourStatesHaveExactCoordinatesAndSurviveDefinitionSnapshotRoundTrip() throws Exception {
        var json = source(MIXED);
        var snapshot = ResearchDefinitions.decode(json, aspects);
        var definition = snapshot.catalog().require("test:layout");
        var note = generate(definition);
        assertEquals(2, note.layoutSize());
        assertEquals(Map.of(CENTER, ResearchNote.Cell.blank(),
                new ResearchHex(-1, 0), new ResearchNote.Cell(1, "terra"),
                new ResearchHex(1, 0), new ResearchNote.Cell(1, "ordo"),
                new ResearchHex(0, -1), new ResearchNote.Cell(3, "ignis"),
                new ResearchHex(-1, 1), ResearchNote.Cell.blank(),
                new ResearchHex(1, -1), ResearchNote.Cell.blank()), note.grid());
        assertFalse(note.grid().containsKey(new ResearchHex(0, 1)));
        // The server sends this complete JSON snapshot; no separate layout codec can drop the field.
        var received = ResearchDefinitions.decode(JsonParser.parseString(snapshot.json().toString()).getAsJsonObject(), aspects);
        assertEquals(definition, received.catalog().require("test:layout"));
        entry(json).remove("noteLayout");
        assertEquals(note, generate(received.catalog().require("test:layout")));
        assertEquals(note, persisted(note));
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void sizeUsesSideLengthAndDefaultEmptyCells(int size) throws Exception {
        var note = generate("{\"size\":" + size + ",\"cells\":[]}");
        assertEquals(List.of(1, 7, 19, 37, 61).get(size - 1).intValue(), note.grid().size());
        assertEquals(size - 1, note.grid().keySet().stream().mapToInt(ResearchHex::radius).max().orElseThrow());
        assertTrue(note.grid().values().stream().allMatch(c -> c.equals(ResearchNote.Cell.blank())));
        assertEquals(0xffffff, note.color());
        assertSame(note, ResearchNotes.expandUndersized(note));
        assertEquals(note, ResearchNotes.expandUndersized(persisted(note)));
        assertTrue(notes.complete(note, tag -> true).isEmpty(), "Do not fabricate anchors on empty layouts");
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void layoutOverridesComplexityLegacySeedsRandomizationAndSingletonDuplication(int complexity) {
        var json = source("""
                {"size":2,"cells":[{"q":0,"r":0,"state":"initial","aspect":"terra"},
                {"q":1,"r":-1,"state":"negative","aspect":"aqua"}]}
                """);
        entry(json).addProperty("complexity", complexity);
        entry(json).add("innerAspects", JsonParser.parseString("[\"ignis\"]"));
        entry(json).add("negativeAspects", JsonParser.parseString("[\"perditio\"]"));
        var definition = ResearchDefinitions.decode(json, aspects).catalog().require("test:layout");
        var original = generate(definition);
        ResearchNoteTestSeeds.set(32, 5);
        assertSame(definition, ResearchNoteTestSeeds.apply(definition, aspects, bound -> { throw new AssertionError(); }));
        assertEquals(original, generate(definition));
        assertEquals(original, notes.generate(definition, bound -> { throw new AssertionError(); }, false).orElseThrow());
        assertEquals(7, original.grid().size());
        assertEquals(1, original.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        assertEquals(new ResearchNote.Cell(1, "terra"), original.grid().get(CENTER));
        assertEquals(new ResearchNote.Cell(3, "aqua"), original.grid().get(new ResearchHex(1, -1)));
    }

    @Test void repeatedAspectsInteriorAnchorsAndAnEntireBlockedBoardArePreserved() throws Exception {
        var note = generate("""
                {"size":3,"cells":[{"q":0,"r":0,"state":"initial","aspect":"terra"},
                {"q":1,"r":0,"state":"initial","aspect":"terra"},
                {"q":0,"r":2,"state":"negative","aspect":"terra"}]}
                """);
        assertEquals(2, note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        assertEquals(1, note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
        assertEquals(note, persisted(note));
        var blocked = generate("{\"size\":1,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"blocked\"}]}");
        assertTrue(blocked.grid().isEmpty());
        assertTrue(persisted(blocked).grid().isEmpty());
        assertTrue(notes.complete(blocked, tag -> true).isEmpty());
    }

    @Test void layoutsWorkWithoutCostAspectsAndDoNotOverridePurchaseOrMirrorRules() {
        var json = source(MIXED); entry(json).remove("aspects");
        var definition = ResearchDefinitions.decode(json, aspects).catalog().require("test:layout");
        assertEquals(6, generate(definition).grid().size());
        assertEquals(ResearchBookActions.Route.NOTE, ResearchBookActions.route(definition, 0));
        json = source(MIXED); entry(json).addProperty("isSecondary", true);
        definition = ResearchDefinitions.decode(json, aspects).catalog().require("test:layout");
        assertEquals(ResearchBookActions.Route.PURCHASE, ResearchBookActions.route(definition, 0));
        assertEquals(ResearchBookActions.Route.NOTE, ResearchBookActions.route(definition, 1));
        entry(json).addProperty("mirrorOf", "RESEARCHDUPE");
        definition = ResearchDefinitions.decode(json, aspects).catalog().require("test:layout");
        assertTrue(notes.generate(definition, bound -> { throw new AssertionError(); }).isEmpty());
    }

    @Test void placeEraseCompleteAndDuplicatePreserveAuthoredSizeAndFixedCells() throws Exception {
        // Keep all seven coordinates: this is precisely the layout the legacy repair used to expand.
        var full = MIXED.replace("{\"q\":0,\"r\":1,\"state\":\"blocked\"}", "{\"q\":0,\"r\":1,\"state\":\"empty\"}");
        var snapshot = decode(full); var definition = snapshot.catalog().require("test:layout");
        var original = generate(definition); var actions = new ResearchTableActions(aspects, snapshot.catalog(), e -> true);
        var player = new PlayerKnowledge(aspects);
        for (String tag : List.of("aer", "vitreus", "terra", "ordo")) player.setPool(aspects.require(tag), 10);
        player.rememberResearch("RESEARCHDUPE");
        var placed = actions.place(table(original), player, CENTER, "aer", () -> .9f).change().orElseThrow();
        assertFalse(placed.completedNow());
        var changed = persisted(placed.table().note().orElseThrow());
        assertEquals(2, changed.layoutSize()); assertEquals(7, ResearchNotes.expandUndersized(changed).grid().size());
        var erased = actions.place(table(changed), placed.knowledge(), CENTER, null, () -> .9f).change().orElseThrow();
        assertEquals(original, persisted(erased.table().note().orElseThrow()));
        var solved = actions.place(erased.table(), erased.knowledge(), CENTER, "vitreus", () -> .9f).change().orElseThrow();
        assertTrue(solved.completedNow());
        var complete = persisted(solved.table().note().orElseThrow());
        assertEquals(2, complete.layoutSize()); assertEquals(4, complete.grid().size());
        assertEquals(new ResearchNote.Cell(3, "ignis"), complete.grid().get(new ResearchHex(0, -1)));
        var copied = actions.duplicate(solved.table(), solved.knowledge(), 1, 1).change().orElseThrow();
        var copy = persisted(copied.table().note().orElseThrow());
        assertEquals(2, copy.layoutSize()); assertEquals(1, copy.copies()); assertEquals(2, copied.table().noteCount());
        assertEquals(complete.grid(), copy.grid());
        assertEquals(7, original.grid().size()); assertFalse(original.complete());
    }

    @Test void blockedOutsideAndFixedCellsRejectWritesAndNegativeLinksStillPreventCompletion() {
        var snapshot = decode(MIXED); var note = generate(snapshot.catalog().require("test:layout"));
        var actions = new ResearchTableActions(aspects, snapshot.catalog(), e -> true);
        var player = new PlayerKnowledge(aspects); player.setPool(aspects.require("vitreus"), 10);
        for (var hex : List.of(new ResearchHex(0, 1), new ResearchHex(0, 2), new ResearchHex(-1, 0), new ResearchHex(0, -1))) {
            assertEquals(Optional.of(ResearchTableActions.Rejection.INVALID_CELL), actions.place(table(note), player, hex, "vitreus", () -> .9f).rejection());
            assertEquals(Optional.of(ResearchTableActions.Rejection.INVALID_CELL), actions.place(table(note), player, hex, null, () -> .9f).rejection());
        }
        snapshot = decode(MIXED.replace("\"aspect\":\"ignis\"", "\"aspect\":\"terra\""));
        note = generate(snapshot.catalog().require("test:layout"));
        actions = new ResearchTableActions(aspects, snapshot.catalog(), e -> true);
        var change = actions.place(table(note), player, CENTER, "vitreus", () -> .9f).change().orElseThrow();
        assertFalse(change.completedNow());
        var connections = ResearchTableView.connections(change.table().note().orElseThrow(), aspects, tag -> true);
        assertTrue(connections.edges().stream().anyMatch(e -> e.to().equals(new ResearchHex(0, -1))));
        assertFalse(connections.edges().stream().anyMatch(e -> e.from().equals(new ResearchHex(0, -1))));
        assertFalse(connections.lit().contains(new ResearchHex(0, 1)));
    }

    @Test void immutableJavaApiAndLegacyConstructorsRemainAvailable() {
        var cells = new LinkedHashMap<ResearchHex, ResearchNoteLayout.Cell>();
        cells.put(CENTER, new ResearchNoteLayout.Cell(ResearchNoteLayout.State.INITIAL, "terra"));
        var layout = new ResearchNoteLayout(2, cells); cells.clear();
        assertEquals(1, layout.cells().size());
        assertThrows(UnsupportedOperationException.class, () -> layout.cells().clear());
        assertThrows(UnsupportedOperationException.class, () -> layout.initialGrid().clear());
        var base = decode(MIXED).catalog().require("test:layout");
        var legacy = new ResearchDefinition(base.key(), base.category(), base.aspects(), base.parents(), base.hiddenParents(),
                base.siblings(), base.flags(), base.complexity(), base.warp(), base.display(), base.triggers(),
                base.parentsAny(), base.noteSeeds(), base.reference());
        assertTrue(legacy.noteLayout().isEmpty());
        var old = new ResearchNote("old", 0, false, 0, layout.initialGrid());
        assertEquals(0, old.layoutSize()); assertEquals(19, ResearchNotes.expandUndersized(old).grid().size());
    }

    @Test void malformedSizeMetadataCannotSilentlyShrinkOrExpandSavedNotes() {
        var note = generate(MIXED); var tag = new CompoundTag(); ResearchNoteNbt.write(tag, note);
        for (int invalid : List.of(-1, 0, 1, 6, Integer.MAX_VALUE)) {
            tag.putInt("layoutSize", invalid);
            assertTrue(ResearchNoteNbt.read(tag, aspects).note().isEmpty(), "Invalid size " + invalid);
        }
        tag.putString("layoutSize", "2"); assertTrue(ResearchNoteNbt.read(tag, aspects).note().isEmpty());
        tag.putByte("layoutSize", (byte) 2); assertTrue(ResearchNoteNbt.read(tag, aspects).note().isEmpty());
        var legacy = new ResearchNote("legacy", 0, false, 0, note.grid());
        ResearchNoteNbt.write(tag, legacy);
        assertFalse(tag.contains("layoutSize"), "Reusing a tag must not retain a stale authored size");
        assertEquals(legacy, ResearchNoteNbt.read(tag, aspects).note().orElseThrow());
    }

    @ParameterizedTest @ValueSource(strings = {
        "null", "[]", "{}", "{\"size\":0,\"cells\":[]}", "{\"size\":6,\"cells\":[]}",
        "{\"size\":-1,\"cells\":[]}", "{\"size\":2.5,\"cells\":[]}", "{\"size\":\"2\",\"cells\":[]}",
        "{\"size\":true,\"cells\":[]}", "{\"size\":2,\"cells\":null}", "{\"size\":2,\"cells\":{}}",
        "{\"size\":2,\"cells\":[] ,\"typo\":0}", "{\"size\":2,\"cells\":[null]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"placed\",\"aspect\":\"terra\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"initial\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"negative\",\"aspect\":\"missing\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"initial\",\"aspect\":\"missing\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"initial\",\"aspect\":\"\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"negative\",\"aspect\":null}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"negative\",\"aspect\":42}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"empty\",\"aspect\":\"terra\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"blocked\",\"aspect\":\"terra\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"empty\",\"aspect\":null}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":null}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"empty\",\"typo\":0}]}",
        "{\"size\":2,\"cells\":[{\"r\":0,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":2,\"r\":0,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":1,\"r\":1,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":-1,\"r\":-1,\"state\":\"blocked\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0.5,\"r\":0,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0.5,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":\"0\",\"r\":0,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":2147483648,\"r\":0,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":-2147483648,\"r\":-2147483648,\"state\":\"empty\"}]}",
        "{\"size\":2,\"cells\":[{\"q\":0,\"r\":0,\"state\":\"empty\"},{\"q\":0,\"r\":0,\"state\":\"blocked\"}]}"
    })
    void invalidLayoutsRejectTheDefinitionBeforePublication(String layout) {
        assertThrows(IllegalArgumentException.class, () -> decode(layout));
    }
}
