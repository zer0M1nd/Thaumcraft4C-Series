package tc4revival.research;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tc4revival.network.BookActionPacket;
import tc4revival.network.BookResultPacket;
import static org.junit.jupiter.api.Assertions.*;

class BookClientStateTest {
    @Test void replacedScreenCannotCloseRequestOrConsumeNewOpening() {
        var state = new BookClientState(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        state.open(first); state.request(first, "THAUMIUM").orElseThrow();
        state.open(second);
        assertTrue(state.close(first).isEmpty()); assertTrue(state.active(second));
        assertTrue(state.request(first, "THAUMIUM").isEmpty());
        assertEquals(0, state.request(second, "THAUMIUM").orElseThrow().sequence());
        state.receive(answer(second, 0, BookResultPacket.Status.NOTE_CREATED));
        assertTrue(state.takeResult(first).isEmpty());
        assertEquals(BookResultPacket.Status.NOTE_CREATED, state.takeResult(second).orElseThrow().status());
        assertTrue(state.takeResult(second).isEmpty());
    }
    @Test void pendingRequestsIgnoreOldTokensUnsolicitedRepliesAndReplays() {
        var state = new BookClientState(); var token = UUID.randomUUID(); state.open(token);
        state.receive(answer(token, 0, BookResultPacket.Status.PURCHASED));
        assertTrue(state.takeResult(token).isEmpty());
        state.request(token, "THAUMIUM").orElseThrow();
        assertTrue(state.request(token, "BASICS").isEmpty());
        state.receive(answer(UUID.randomUUID(), 0, BookResultPacket.Status.CLOSED));
        state.receive(answer(token, 1, BookResultPacket.Status.CLOSED));
        assertTrue(state.waiting()); assertTrue(state.active(token));
        state.receive(answer(token, 0, BookResultPacket.Status.PURCHASE_REJECTED));
        assertFalse(state.waiting()); assertTrue(state.active(token));
        assertEquals(1, state.request(token, "THAUMIUM").orElseThrow().sequence());
        state.receive(answer(token, 0, BookResultPacket.Status.PURCHASED));
        assertTrue(state.waiting()); assertTrue(state.takeResult(token).isEmpty());
    }
    @Test void terminalResponseIsAvailableToOwnerAfterAuthorizationEnds() {
        var state = new BookClientState(); var token = UUID.randomUUID(); state.open(token);
        state.request(token, "THAUMIUM").orElseThrow();
        state.receive(answer(token, 0, BookResultPacket.Status.AUTH_REJECTED));
        assertFalse(state.active(token)); assertFalse(state.waiting());
        assertEquals(BookResultPacket.Status.AUTH_REJECTED, state.takeResult(token).orElseThrow().status());
        assertTrue(state.close(token).isEmpty()); assertTrue(state.request(token, "THAUMIUM").isEmpty());
    }
    @Test void invalidIntentDoesNotAdvanceSequenceAndCloseUsesNextSequenceWhilePending() {
        var state = new BookClientState(); var token = UUID.randomUUID(); state.open(token);
        assertThrows(IllegalArgumentException.class, () -> state.request(token, ""));
        assertFalse(state.waiting());
        assertEquals(0, state.request(token, "THAUMIUM").orElseThrow().sequence());
        var close = state.close(token).orElseThrow();
        assertEquals(BookActionPacket.Action.CLOSE, close.action()); assertEquals(1, close.sequence());
        state.receive(answer(token, 0, BookResultPacket.Status.PURCHASED));
        assertTrue(state.token().isEmpty()); assertTrue(state.takeResult(token).isEmpty());
        state.open(token); state.request(token, "THAUMIUM").orElseThrow(); state.clear();
        assertFalse(state.waiting()); assertTrue(state.token().isEmpty());
    }
    private static BookResultPacket answer(UUID token, long sequence, BookResultPacket.Status status) {
        return new BookResultPacket(token, sequence, status, "");
    }
}
