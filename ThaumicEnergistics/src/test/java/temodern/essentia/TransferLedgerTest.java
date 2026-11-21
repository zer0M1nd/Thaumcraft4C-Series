package temodern.essentia;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferLedgerTest {
    @Test void rejectedDestinationRetainsEverythingAcrossRetry() {
        var ledger = new TransferLedger<String>(); ledger.retain("aer", 17);
        assertEquals(0, ledger.deliver("aer", requested -> 0));
        assertEquals(17, ledger.amount("aer"));
        assertEquals(5, ledger.deliver("aer", requested -> 5));
        assertEquals(12, ledger.amount("aer"));
        var restored = new TransferLedger<String>(); ledger.snapshot().forEach(restored::retain);
        assertEquals(12, restored.deliver("aer", requested -> requested));
        assertTrue(restored.snapshot().isEmpty());
    }
    @Test void destinationCannotReportAnImpossibleTransfer() {
        var ledger = new TransferLedger<String>(); ledger.retain("aer", 4);
        assertThrows(IllegalStateException.class, () -> ledger.deliver("aer", requested -> 5));
        assertEquals(4, ledger.amount("aer"));
        assertThrows(IllegalStateException.class, () -> ledger.deliver("aer", requested -> -1));
        assertEquals(4, ledger.amount("aer"));
    }
    @Test void overflowDoesNotDestroyExistingEscrow() {
        var ledger = new TransferLedger<String>(); ledger.retain("aer", Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> ledger.retain("aer", 1));
        assertEquals(Long.MAX_VALUE, ledger.amount("aer"));
        assertThrows(IllegalArgumentException.class, () -> ledger.retain("aer", -1));
    }
    @Test void failuresKeepOtherResourcesAndSnapshotIsImmutable() {
        var ledger = new TransferLedger<String>(); ledger.retain("aer", 4); ledger.retain("ignis", 8);
        assertThrows(IllegalStateException.class, () -> ledger.deliver("aer", n -> { throw new IllegalStateException(); }));
        assertEquals(4, ledger.amount("aer")); assertEquals(8, ledger.amount("ignis"));
        assertThrows(UnsupportedOperationException.class, () -> ledger.snapshot().put("aer", 0L));
    }
}
