package tc4revival.research;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchCompoundLayoutTest {
    private ResearchPageRecipes.Compound structure(int x, int y, int z) {
        return new ResearchPageRecipes.Compound(Map.of(), x, y, z,
                new ResearchPage.Sequence(Collections.nCopies(x * y * z, new ResearchPage.Empty())));
    }
    @Test void layerOrderPreservesEmptyCellsAndOriginalProjection() {
        var layout = ResearchCompoundLayout.of(structure(2, 2, 2));
        assertEquals(1f, layout.scale()); assertEquals(32f, layout.x()); assertEquals(58f, layout.y());
        assertEquals(List.of(new ResearchCompoundLayout.Cell(0, 32, 0, 60),
                new ResearchCompoundLayout.Cell(1, 16, 8, 60),
                new ResearchCompoundLayout.Cell(2, 16, -8, 60),
                new ResearchCompoundLayout.Cell(3, 0, 0, 60),
                new ResearchCompoundLayout.Cell(4, 32, 50, 50),
                new ResearchCompoundLayout.Cell(5, 16, 58, 50),
                new ResearchCompoundLayout.Cell(6, 16, 42, 50),
                new ResearchCompoundLayout.Cell(7, 0, 50, 50)), layout.cells());
    }
    @Test void tallerStructuresUseLegacyScaleAndBaseOffset() {
        var layout = ResearchCompoundLayout.of(structure(3, 4, 3));
        assertEquals(.8f, layout.scale(), .0001f);
        assertEquals(19.2f, layout.x(), .0001f); assertEquals(28f, layout.y(), .0001f);
        assertEquals(-24, layout.baseX()); assertEquals(105, layout.baseY());
        assertEquals(36, layout.cells().size());
    }
    @Test void observedStructuresKeepEveryCellAndPositiveDisplayScale() throws Exception {
        try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(getClass()
                .getResourceAsStream("/research-page-recipes.json")), java.nio.charset.StandardCharsets.UTF_8)) {
            var recipes = ResearchPageRecipes.decode(com.google.gson.JsonParser.parseReader(reader).getAsJsonArray());
            int count = 0;
            for (var entry : recipes.entries().values()) {
                if (!(entry.definition() instanceof ResearchPageRecipes.Compound structure)) continue;
                count++;
                var layout = ResearchCompoundLayout.of(structure);
                assertEquals(structure.blocks().elements().size(), layout.cells().size(), entry.key());
                assertTrue(layout.scale() > 0, entry.key());
                for (int i = 0; i < layout.cells().size(); i++) assertEquals(i, layout.cells().get(i).index());
            }
            assertTrue(count > 0);
        }
    }
}
