package tc4revival.research;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeTransactionTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void replacementOwnsItsCopyAndRejectsStaleOrUnknownPlayer() {
        var storage = new KnowledgeSavedData(catalog);
        var id = UUID.randomUUID(); storage.ensure(id, bound -> 0);
        var proposed = storage.snapshot(id).orElseThrow();
        proposed.setPool(catalog.require("aer"), (short) 42);
        long revision = storage.revision(id);
        assertFalse(storage.replace(UUID.randomUUID(), 0, proposed));
        assertFalse(storage.replace(id, revision - 1, proposed));
        assertNotEquals(42, storage.snapshot(id).orElseThrow().pool(catalog.require("aer")));
        assertTrue(storage.replace(id, revision, proposed));
        proposed.setPool(catalog.require("aer"), (short) 99);
        assertEquals(42, storage.snapshot(id).orElseThrow().pool(catalog.require("aer")));
        assertEquals(revision + 1, storage.revision(id));
        assertFalse(storage.replace(id, revision, proposed));
    }
}
