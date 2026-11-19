package thaumicomniverse;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import thaumicomniverse.research.ResearchLayout;
import static org.junit.jupiter.api.Assertions.*;

class ResearchLayoutTest {
    private JsonArray catalog(Set<String> loaded) throws Exception {
        var result = new JsonArray();
        try (var paths = Files.list(Path.of("src/main/resources/data/thaumicomniverse/research_catalog"))) {
            for (var path : paths.sorted().toList()) {
                var cats = JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("categories");
                for (var element : cats) {
                    var category = element.getAsJsonObject();
                    if (!category.get("key").getAsString().equals("THAUMICOMNIVERSE")) continue;
                    if (!enabled(category, loaded)) continue;
                    var nodes = new JsonArray();
                    // Exercise the retained legacy packing algorithm on gameplay nodes only.
                    for (var row : category.getAsJsonArray("research")) {
                        var data=row.getAsJsonObject();
                        if (!data.has("mirrorOf") && !(data.has("isStub") && data.has("isAutoUnlock")) && enabled(data, loaded)) nodes.add(row);
                    }
                    category.add("research", nodes); result.add(category);
                }
            }
        }
        return result;
    }
    private boolean enabled(JsonObject row, Set<String> loaded) {
        if (loaded == null) return true;
        if (row.has("omniverse_requires")) for (var id : row.getAsJsonArray("omniverse_requires"))
            if (!loaded.contains(id.getAsString())) return false;
        if (row.has("omniverse_requires_any")) {
            boolean any = false;
            for (var id : row.getAsJsonArray("omniverse_requires_any")) any |= loaded.contains(id.getAsString());
            if (!any) return false;
        }
        return true;
    }
    private TreeMap<String, JsonObject> nodes(JsonArray catalog) {
        var result = new TreeMap<String, JsonObject>();
        for (var category : catalog) for (var element : category.getAsJsonObject().getAsJsonArray("research")) {
            var row = element.getAsJsonObject(); result.put(row.get("key").getAsString(), row);
        }
        return result;
    }
    private int x(JsonObject r) { return r.get("displayColumn").getAsInt(); }
    private int y(JsonObject r) { return r.get("displayRow").getAsInt(); }
    private int[] bounds(Collection<JsonObject> rows) {
        return new int[]{rows.stream().mapToInt(this::x).min().orElse(0), rows.stream().mapToInt(this::y).min().orElse(0),
                rows.stream().mapToInt(this::x).max().orElse(0), rows.stream().mapToInt(this::y).max().orElse(0)};
    }
    private void verify(JsonArray catalog) {
        var before = catalog.deepCopy();
        ResearchLayout.apply(catalog);
        var rows = nodes(catalog); var old = nodes(before); var coordinates = new HashSet<String>();
        for (var row : rows.entrySet()) {
            assertTrue(coordinates.add(x(row.getValue()) + "," + y(row.getValue())), "Duplicate: " + row.getKey());
            var data = row.getValue().deepCopy(); var previous = old.get(row.getKey()).deepCopy();
            for (String field : List.of("displayColumn", "displayRow")) { data.remove(field); previous.remove(field); }
            assertEquals(previous, data, "Layout changed progression or content");
        }
        int[] b = bounds(rows.values()); int w = b[2] - b[0], h = b[3] - b[1];
        double density = (double) rows.size() / (w * h);
        assertTrue(density >= ResearchLayout.MIN_DENSITY && density <= ResearchLayout.MAX_DENSITY, "density=" + density);
        // Also cover the bounding rectangle including the research grid cells themselves.
        double cells = (double) rows.size() / ((w + 1) * (h + 1));
        assertTrue(cells >= 29d / (19 * 12) && cells <= 49d / (17 * 16), "cell density=" + cells);
        var groups = new TreeMap<String, List<JsonObject>>();
        rows.values().forEach(r -> groups.computeIfAbsent(ResearchLayout.group(r), k -> new ArrayList<>()).add(r));
        groups.forEach((name, group) -> {
            int[] box = bounds(group);
            for (var row : rows.values()) if (!ResearchLayout.group(row).equals(name))
                assertFalse(x(row) >= box[0] && x(row) <= box[2] && y(row) >= box[1] && y(row) <= box[3], "Interleaved mod: " + name);
        });
        for (var row : rows.values()) if (row.has("parents")) for (var parent : row.getAsJsonArray("parents")) {
            var target = rows.get(parent.getAsString());
            if (target != null) assertTrue(Math.hypot(x(row)-x(target), y(row)-y(target)) <= 8, "Distant link: " + row.get("key") + " -> " + parent);
        }
        var resolved = catalog.deepCopy(); ResearchLayout.apply(catalog);
        assertEquals(resolved, catalog, "Reload moved research");
    }
    @Test void densityLimitsMatchTheOriginalSixPages() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/thaumcraft/research_catalog/legacy.json")) {
            var cats = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("categories");
            double min = Double.POSITIVE_INFINITY, max = 0;
            for (var c : cats) {
                var one = new JsonArray(); one.add(c); var rows = nodes(one); int[] b = bounds(rows.values());
                double d = (double) rows.size() / ((b[2]-b[0])*(b[3]-b[1])); min = Math.min(min,d); max = Math.max(max,d);
            }
            assertEquals(min, ResearchLayout.MIN_DENSITY); assertEquals(max, ResearchLayout.MAX_DENSITY);
        }
    }
    @Test void allOptionalResearchIsUniqueGroupedAndConnected() throws Exception { verify(catalog(null)); }
    @Test void missingModsDoNotLeaveASparsePage() throws Exception {
        verify(catalog(Set.of()));
        for (String mod : List.of("create", "gtceu", "quark", "avaritia", "sophisticatedbackpacks", "railcraft", "thermal_expansion", "draconicevolution")) verify(catalog(Set.of(mod)));
    }
    @Test void resourceOrderDoesNotChangePositionsAndOtherPagesAreUntouched() throws Exception {
        var cat = catalog(null); var reversed = new JsonArray();
        for (int i=cat.size()-1;i>=0;i--) reversed.add(cat.get(i).deepCopy());
        var other = JsonParser.parseString("{\"key\":\"BASICS\",\"research\":[{\"key\":\"TEST\",\"displayColumn\":1,\"displayRow\":2}]}").getAsJsonObject();
        cat.add(other); var saved = other.deepCopy();
        ResearchLayout.apply(cat); ResearchLayout.apply(reversed);
        var actual = nodes(cat); actual.remove("TEST");
        assertEquals(nodes(reversed), actual); assertEquals(saved,other);
    }
    @Test void migratedThaumiumMaterialHasItsActualTranslationKey() throws Exception {
        for (String language : List.of("zh_cn", "en_us")) {
            var path = Path.of("src/main/resources/assets/thaumicomniverse/lang", language + ".json");
            var lang = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            assertEquals(language.equals("zh_cn") ? "神秘金属" : "Thaumium", lang.get("material.thaumicfutures.thaumium").getAsString());
            assertEquals(language.equals("zh_cn") ? "神秘" : "Thaumic", lang.get("modifier.thaumicfutures.thaumic").getAsString());
            for (String suffix : List.of("flavor", "description")) assertFalse(lang.get("modifier.thaumicfutures.thaumic." + suffix).getAsString().isBlank());
        }
    }
}
