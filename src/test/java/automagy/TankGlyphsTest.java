package automagy;
import automagy.logic.TankGlyphs;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TankGlyphsTest {
 @Test void stacksCapacitySpeedAndConsumption(){var g=TankGlyphs.from(new int[]{8,8,7,7,2,2});assertEquals(48000,g.capacity());assertEquals(13,g.drinkInterval());assertEquals(200,g.consumption());assertFalse(g.voiding());}
 @Test void reservationAppliesToDrainingConsumingAndSiphoning(){var g=TankGlyphs.from(new int[]{6,1,5,9,0,0});assertTrue(g.voiding());assertTrue(g.controlled());assertTrue(g.milking());assertEquals(0,g.drainLimit(1000,1));assertEquals(999,g.drainLimit(1000,1000));assertEquals(50,g.siphonShare(1000,3));assertEquals(0,g.siphonShare(2,3));}
 @Test void capacityGlyphCannotDeleteStoredLiquid(){var g=TankGlyphs.from(new int[]{8,0,0,0,0,0});assertTrue(g.canRemoveCapacity(16000));assertFalse(g.canRemoveCapacity(16001));}
}
