package automagy.logic;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestLedgerTest {
    @Test void competingCarriersCannotClaimTheSameDemand(){
        var ledger=new RequestLedger<String,String>();ledger.request("iron","desk",10);
        var first=ledger.claim("iron","desk",8).orElseThrow();var second=ledger.claim("iron","desk",8).orElseThrow();
        assertEquals(8,first.amount());assertEquals(2,second.amount());assertTrue(ledger.claim("iron","desk",1).isEmpty());
        assertEquals(10,ledger.requested("iron","desk"));assertEquals(10,ledger.inTransit("iron","desk"));
    }
    @Test void partialDeliveryAndFailedTaskReleaseUndeliveredWork(){
        var ledger=new RequestLedger<String,String>();ledger.request("iron","desk",10);var task=ledger.claim("iron","desk",8).orElseThrow();
        assertEquals(3,ledger.finish(task.id(),3));assertEquals(7,ledger.available("iron","desk"));assertEquals(0,ledger.finish(task.id(),3));
        var retry=ledger.claim("iron","desk",7).orElseThrow();ledger.fail(retry.id());assertEquals(7,ledger.available("iron","desk"));
    }
    @Test void cancellationPreservesCarrierOwnershipAndRestorationDropsGhostTasks(){
        var ledger=new RequestLedger<String,String>();ledger.request("iron","desk",10);var task=ledger.claim("iron","desk",8).orElseThrow();
        ledger.cancel("iron","desk",10);assertEquals(0,ledger.available("iron","desk"));assertEquals(1,ledger.claims().size());
        assertEquals(0,ledger.deliveryLimit(task.id()));
        var restored=new RequestLedger<String,String>();restored.restore(ledger.requests(),ledger.claims(),Set.of(task.id()));assertEquals(8,restored.inTransit("iron","desk"));
        restored.restore(ledger.requests(),ledger.claims(),Set.of());assertTrue(restored.claims().isEmpty());
        assertEquals(0,ledger.finish(task.id(),8));
    }
    @Test void destinationsAndOverflowAreIndependent(){
        var ledger=new RequestLedger<String,String>();ledger.request("iron","a",Long.MAX_VALUE);ledger.request("iron","a",10);ledger.request("iron","b",4);
        assertEquals(Long.MAX_VALUE,ledger.requested("iron","a"));assertEquals(4,ledger.claim("iron","b",20).orElseThrow().amount());
    }
}
