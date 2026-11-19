package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NaturalAuraQueuePersistenceTest {
    @Test void completedChunksSurviveSavingAndReloading() {
        var pending = new NaturalAuraQueue.Pending();
        pending.completed.add(123L);
        pending.entries.put(456L, 789L);
        var restored = NaturalAuraQueue.Pending.load(pending.save(new CompoundTag()));
        assertTrue(restored.completed.contains(123L));
        assertEquals(789L, restored.entries.get(456L));
        assertNull(restored.unreadable);
    }

    @Test void oldSavesWithoutCompletionHistoryRetainPendingWork() {
        var tag = new CompoundTag();
        var candidates = new ListTag();
        var row = new CompoundTag();
        row.putLong("Chunk", 123L);
        row.putLong("Seed", 456L);
        candidates.add(row);
        tag.put("Candidates", candidates);
        var restored = NaturalAuraQueue.Pending.load(tag);
        assertEquals(456L, restored.entries.get(123L));
        assertTrue(restored.completed.isEmpty());
        assertNull(restored.unreadable);
    }

    @Test void malformedCompletionHistoryIsPreservedWithoutExecutingWork() {
        var tag = new CompoundTag();
        tag.put("Candidates", new ListTag());
        tag.putString("Completed", "unexpected data");
        var restored = NaturalAuraQueue.Pending.load(tag);
        assertNotNull(restored.unreadable);
        assertTrue(restored.entries.isEmpty());
        assertEquals(tag, restored.save(new CompoundTag()));
    }
}
