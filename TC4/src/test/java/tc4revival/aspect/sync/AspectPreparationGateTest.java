package tc4revival.aspect.sync;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AspectPreparationGateTest {
    @Test void loginWaitsForBothRecipesAndTags(){
        var gate=new AspectPreparationGate();gate.resources();gate.recipes();assertFalse(gate.ready());
        gate.tags();assertTrue(gate.ready());gate.start();assertFalse(gate.ready());
    }
    @Test void reloadAcceptsTagsBeforeRecipesButDoesNotReusePreviousGeneration(){
        var gate=new AspectPreparationGate();gate.tags();gate.recipes();gate.resources();gate.start();gate.reset();
        gate.resources();gate.tags();assertFalse(gate.ready());gate.recipes();assertTrue(gate.ready());
    }
    @Test void earlyPreparationAndRepeatedStartAreRejected(){
        var gate=new AspectPreparationGate();assertThrows(IllegalStateException.class,gate::start);
        gate.recipes();gate.tags();gate.resources();gate.start();assertThrows(IllegalStateException.class,gate::start);
    }
}
