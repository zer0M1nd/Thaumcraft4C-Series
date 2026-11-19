package automagy;
import automagy.logic.BoilerCycle;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BoilerCycleTest {
    @Test void consumesFuelOnlyForACompleteBatchThatFits(){
        var cycle=new BoilerCycle();var consumed=new AtomicInteger();java.util.function.Supplier<BoilerCycle.Fuel> fuel=()->{consumed.incrementAndGet();return new BoilerCycle.Fuel(100,false);};
        assertFalse(cycle.tick(999,0,4,0,fuel));assertFalse(cycle.tick(1000,48,4,0,fuel));assertFalse(cycle.tick(1000,0,0,0,fuel));assertEquals(0,consumed.get());
        for(int i=1;i<=40;i++)assertEquals(i==40,cycle.tick(1000,0,4,0,fuel));assertEquals(1,consumed.get());
    }
    @Test void alumentumOnlyChangesOutputIntervalAndRestoresBurnState(){
        var cycle=new BoilerCycle();assertFalse(cycle.tick(1000,0,4,0,()->new BoilerCycle.Fuel(200,true)));assertEquals(20,cycle.outputInterval());
        var restored=new BoilerCycle();restored.restore(cycle.state());assertEquals(cycle.state(),restored.state());
        for(int i=2;i<=40;i++)assertEquals(i==40,restored.tick(1000,0,4,0,()->{throw new AssertionError("Extra fuel");}));
        assertEquals(10,BoilerCycle.duration(4,6));assertEquals(40,BoilerCycle.duration(4,0));
    }
    @Test void interruptedBatchResetsProgressButFuelStillBurns(){
        var cycle=new BoilerCycle();cycle.tick(1000,0,4,0,()->new BoilerCycle.Fuel(100,false));
        assertFalse(cycle.tick(0,0,4,0,()->{throw new AssertionError();}));assertEquals(0,cycle.state().cook());assertEquals(99,cycle.state().burn());
    }
}
