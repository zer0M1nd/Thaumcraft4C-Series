package gadomancymodern.automation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PackagingCycleTest {
    @Test void requiresActualRisingEdge(){
        var cycle=new PackagingCycle();cycle.powerChanged(true,true);assertEquals(-1,cycle.progress());
        cycle.powerChanged(false,true);cycle.powerChanged(true,true);assertEquals(0,cycle.progress());
        for(int i=0;i<47;i++)assertFalse(cycle.tick(false,true,i));assertTrue(cycle.tick(false,true,47));assertEquals(-1,cycle.progress());
        cycle.powerChanged(true,true);assertEquals(-1,cycle.progress());
    }
    @Test void rechecksInputsAtCommit(){
        var cycle=new PackagingCycle();cycle.tick(true,true,0);
        for(int i=0;i<47;i++)assertFalse(cycle.tick(false,true,i));
        assertFalse(cycle.tick(false,false,47));assertEquals(-1,cycle.progress());
    }
    @Test void triggerDoesNotRestartAnActiveCycle(){
        var cycle=new PackagingCycle();cycle.powerChanged(false,true);cycle.powerChanged(true,true);
        cycle.tick(false,true,1);cycle.powerChanged(false,true);cycle.powerChanged(true,true);assertEquals(1,cycle.progress());
    }
}
