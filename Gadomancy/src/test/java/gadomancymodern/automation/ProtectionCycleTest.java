package gadomancymodern.automation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class ProtectionCycleTest {
    @Test void saturationDelaysGrowthAndShutdown(){var cycle=new ProtectionCycle();int[] consumed={0};assertTrue(cycle.advance(15,()->{consumed[0]++;return true;}));assertEquals(1,cycle.range());assertEquals(15,cycle.saturation());for(int i=0;i<15;i++)assertFalse(cycle.advance(0,()->{fail("Saturation does not spend fuel");return false;}));assertEquals(1,cycle.range());assertTrue(cycle.advance(0,()->false));assertEquals(0,cycle.range());assertEquals(1,consumed[0]);}
    @Test void maintainedRangeSpendsAndFuelLossShrinks(){var cycle=new ProtectionCycle();cycle.restore(3,0);assertTrue(cycle.advance(3,()->true));assertEquals(3,cycle.range());assertEquals(0,cycle.saturation());cycle.advance(3,()->false);assertEquals(2,cycle.range());cycle.advance(4,()->true);assertEquals(3,cycle.range());assertEquals(13,cycle.saturation());}
    @Test void savedBoundsCannotCreateUnboundedProtection(){var cycle=new ProtectionCycle();cycle.restore(Integer.MAX_VALUE,Integer.MAX_VALUE);assertEquals(15,cycle.range());assertEquals(15,cycle.saturation());cycle.restore(-3,-4);assertEquals(0,cycle.range());assertEquals(0,cycle.saturation());}
}
