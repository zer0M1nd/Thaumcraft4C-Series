package automagy.logic;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ThaumostaticCycleTest {
 @Test void acceptedOnlyEnergyAndPoweredTime(){var cycle=new ThaumostaticCycle();cycle.insert("potentia",1);var fuel=new AtomicInteger();List<IntUnaryOperator> receivers=List.of(n->0,n->{fuel.addAndGet(n);return n;});for(int i=0;i<100;i++)cycle.tick(false,2,receivers);for(int i=0;i<29;i++)assertFalse(cycle.tick(true,2,receivers));assertTrue(cycle.tick(true,2,receivers));assertEquals(1,fuel.get());assertEquals(0,cycle.amount("potentia"));}
 @Test void boostersPauseWithoutVisAndPersist(){var cycle=new ThaumostaticCycle();for(String a:ThaumostaticCycle.ASPECTS)cycle.insert(a,1);cycle.tick(true,6,List.of());assertEquals(90,cycle.range(6));var state=cycle.snapshot();for(int i=0;i<200;i++)cycle.tick(false,6,List.of());assertEquals(state,cycle.snapshot());var restored=new ThaumostaticCycle();restored.restore(state);assertEquals(state,restored.snapshot());}
 @Test void independentCapacityAndNoActiveExtraction(){var c=new ThaumostaticCycle();for(String a:ThaumostaticCycle.ASPECTS){for(int i=0;i<8;i++)assertEquals(1,c.insert(a,1));assertEquals(0,c.insert(a,1));}assertEquals(0,c.insert("ignis",1));assertEquals(0,c.extract("aer",1,true));assertEquals(1,c.extract("aer",1,false));}
 @Test void emptyEnergyDoesNotSpendBoostFuel(){var c=new ThaumostaticCycle();c.insert("aer",1);c.insert("volatus",1);c.tick(true,6,List.of());assertEquals(24,c.range(6));assertFalse(c.rateBoost());assertEquals(1,c.amount("aer"));}
}
