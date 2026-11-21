package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchRequirementsTest {
    @Test void directHiddenAndAlternativeParentsStayDistinctWithoutDuplicateRequiredRows() {
        var entry = new ResearchDefinition("TEST", "BASICS", Map.of(), List.of("A", "B"), List.of("B", "HIDDEN"), List.of("SIBLING"),
                Set.of(), 1, 0, new ResearchDefinition.Display(0, 0, Optional.empty(), Optional.empty(), 0),
                new ResearchDefinition.Triggers(Optional.empty(), Optional.empty(), Optional.empty()), List.of("B", "ANY"));
        var rows = ResearchRequirements.collect(entry);
        assertEquals(List.of("A", "B", "HIDDEN", "ANY"), rows.stream().map(ResearchRequirements.Requirement::key).toList());
        assertEquals(List.of(false, false, false, true), rows.stream().map(ResearchRequirements.Requirement::alternative).toList());
    }
    @Test void originalScanAlternativesIncludeEveryFlattenedTriggerAndSpecialCondition() throws Exception {
        var aspects = new tc4revival.aspect.AspectCatalog(Arrays.stream(tc4revival.aspect.LegacyAspect.values()).map(tc4revival.aspect.LegacyAspect::create).toList());
        ResearchCatalog catalog;
        try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/research-catalog.json")), java.nio.charset.StandardCharsets.UTF_8)) {
            catalog = ResearchCatalog.decode(com.google.gson.JsonParser.parseReader(reader).getAsJsonArray(), aspects);
        }
        var mirror = ResearchRequirements.collect(catalog.require("MIRROR"));
        assertEquals(4, mirror.stream().filter(r -> r.kind() == ResearchRequirements.Kind.ITEM).count());
        assertTrue(mirror.stream().filter(r -> r.kind() != ResearchRequirements.Kind.RESEARCH).allMatch(ResearchRequirements.Requirement::alternative));
        assertTrue(mirror.stream().anyMatch(r -> r.kind() == ResearchRequirements.Kind.ENTITY && r.key().equals("Enderman")));
    }
    @Test void unfinishedRowsSortFirstAndCompletedRowsRetainAuthoredOrder() {
        var page = ResearchRequirements.page(List.of("done1", "missing1", "done2", "missing2"), s -> s.startsWith("done"), 5);
        assertEquals(List.of("missing1", "missing2", "done1", "done2"), page.rows().stream().map(ResearchRequirements.Row::value).toList());
        assertEquals(List.of(false, false, true, true), page.rows().stream().map(ResearchRequirements.Row::complete).toList());
        assertFalse(page.truncated());
    }
    @Test void truncationReservesOneLineAndCountsEachOmittedState() {
        var page = ResearchRequirements.page(List.of(0, 1, 2, 3, 4, 5), n -> n % 2 == 0, 3);
        assertEquals(List.of(1, 3), page.rows().stream().map(ResearchRequirements.Row::value).toList());
        assertEquals(3, page.omittedComplete()); assertEquals(1, page.omittedIncomplete());
    }
    @Test void exactlyFullAndEmptyPagesDoNotInventAnOmissionLine() {
        assertFalse(ResearchRequirements.page(List.of(1, 2, 3), n -> true, 3).truncated());
        assertTrue(ResearchRequirements.page(List.of(), n -> false, 3).rows().isEmpty());
        var one = ResearchRequirements.page(List.of(1, 2), n -> false, 1);
        assertTrue(one.rows().isEmpty()); assertEquals(2, one.omittedIncomplete());
        assertThrows(IllegalArgumentException.class, () -> ResearchRequirements.page(List.of(), n -> true, 0));
    }
}
