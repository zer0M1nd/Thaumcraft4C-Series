package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfusionCommitTest {
    private static class Resource implements InfusionCommit.Operation {
        int amount=1,notifications;boolean valid=true,failAfterWrite,throwAfterWrite,failNotify;
        public boolean validate(){return valid;}
        public boolean commit(){amount=0;if(throwAfterWrite)throw new IllegalStateException("injected write failure");return !failAfterWrite;}
        public void abort(){amount=1;}
        public void completed(){notifications++;if(failNotify)throw new IllegalStateException("injected observer failure");}
    }
    @Test void failedValidationDoesNotSpendOrDeliver(){
        var crystal=new Resource();var delivery=new Resource();delivery.valid=false;
        assertFalse(InfusionCommit.execute(List.of(()->crystal),delivery));
        assertEquals(1,crystal.amount);assertEquals(1,delivery.amount);assertEquals(0,crystal.notifications);
    }
    @Test void failedDeliveryRollsBackEveryParticipantIncludingPartialWriter(){
        var crystal=new Resource();var delivery=new Resource();delivery.failAfterWrite=true;
        assertFalse(InfusionCommit.execute(List.of(()->crystal),delivery));
        assertEquals(1,crystal.amount);assertEquals(1,delivery.amount);assertEquals(0,crystal.notifications);
    }
    @Test void exceptionDuringResourceCommitRollsBackAndNeverNotifies(){
        var first=new Resource();var second=new Resource();second.throwAfterWrite=true;var delivery=new Resource();
        assertFalse(InfusionCommit.execute(List.of(()->first,()->second),delivery));
        assertEquals(1,first.amount);assertEquals(1,second.amount);assertEquals(1,delivery.amount);
        assertEquals(0,first.notifications+second.notifications+delivery.notifications);
    }
    @Test void observersCannotRevokeSuccessfulDeliveryOrSuppressLaterObservers(){
        var crystal=new Resource();crystal.failNotify=true;var delivery=new Resource();
        assertTrue(InfusionCommit.execute(List.of(()->crystal),delivery));
        assertEquals(0,crystal.amount);assertEquals(0,delivery.amount);
        assertEquals(1,crystal.notifications);assertEquals(1,delivery.notifications);
    }
    @Test void rollbackReversesPreparationOrder(){
        var order=new ArrayList<String>();
        class Step extends Resource {final String id;Step(String id){this.id=id;}public void abort(){order.add(id);}}
        var delivery=new Step("delivery");delivery.valid=false;
        assertFalse(InfusionCommit.execute(List.of(()->new Step("a"),()->new Step("b")),delivery));
        assertEquals(List.of("delivery","b","a"),order);
    }
    @Test void preparationExceptionAbortsAlreadyPreparedResources(){
        var resource=new Resource();var calls=new int[1];
        Supplier<InfusionCommit.Operation> first=()->new Resource(){public void abort(){calls[0]++;}};
        assertFalse(InfusionCommit.execute(List.of(first,()->{throw new IllegalArgumentException("invalid preparation");}),resource));
        assertEquals(1,calls[0]);assertEquals(1,resource.amount);assertEquals(0,resource.notifications);
    }
}
