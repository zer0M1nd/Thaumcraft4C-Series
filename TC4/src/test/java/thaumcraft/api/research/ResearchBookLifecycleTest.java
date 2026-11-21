package thaumcraft.api.research;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBookLifecycleTest {
    @Test void brokenDiagnosticCannotInterruptBookOpeningOrFollowingObservers(){
        var seen=new AtomicInteger();
        ResearchBookLifecycle.registerObserver("test:broken",100,event->{throw new IllegalStateException("diagnostic only");});
        ResearchBookLifecycle.registerObserver("test:working",0,event->seen.incrementAndGet());
        assertDoesNotThrow(()->ResearchBookLifecycle.observe(ResearchBookLifecycle.Stage.SESSION_CREATED,null,null,"test"));
        assertEquals(1,seen.get());
    }
}
