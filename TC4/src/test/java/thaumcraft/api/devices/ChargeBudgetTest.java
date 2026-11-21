package thaumcraft.api.devices;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChargeBudgetTest {
    @Test void nativeAndAddonPaymentsShareOneTickAllowance(){
        var budget=new ChargeBudget<String>(5);
        assertEquals(3,budget.pay(1,"aer",3,n->n));assertEquals(2,budget.pay(1,"aer",5,n->n));
        assertEquals(0,budget.pay(1,"aer",1,n->{fail("exhausted budget must not drain");return n;}));
        assertEquals(5,budget.pay(1,"ignis",10,n->n));assertEquals(5,budget.allowance(2,"aer",20));
    }
    @Test void simulationAndShortSupplyDoNotSpendUnavailableVis(){
        var budget=new ChargeBudget<String>(5);assertEquals(5,budget.allowance(1,"aer",10));assertEquals(5,budget.allowance(1,"aer",10));
        assertEquals(2,budget.pay(1,"aer",5,n->2));assertEquals(3,budget.allowance(1,"aer",5));
        assertEquals(0,budget.pay(1,"aer",3,n->0));assertEquals(3,budget.pay(1,"aer",5,n->n));
    }
    @Test void reentrantSourceCannotDoubleSpendBudget(){
        var budget=new ChargeBudget<String>(5);
        assertEquals(5,budget.pay(1,"aer",5,n->{assertEquals(0,budget.pay(1,"aer",5,x->x));return n;}));
    }
    @Test void exceptionalSourceRetainsReservationToPreventRetryDuplication(){
        var budget=new ChargeBudget<String>(5);
        assertThrows(IllegalStateException.class,()->budget.pay(1,"aer",5,n->{throw new IllegalStateException();}));
        assertEquals(0,budget.allowance(1,"aer",5));assertEquals(5,budget.allowance(2,"aer",5));
    }
}
