package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SensorRangeTest {
    @Test void openAxisIncludesFiveBlocksBeyondEitherFace(){assertEquals(new SensorRange.Span(-5,6),SensorRange.axis(false,false));}
    @Test void oneBlockedSideRedirectsItsReach(){assertEquals(new SensorRange.Span(0,11),SensorRange.axis(true,false));assertEquals(new SensorRange.Span(-10,1),SensorRange.axis(false,true));}
    @Test void twoBlockedSidesRestrictTheAxisToTheSensorBlock(){assertEquals(new SensorRange.Span(0,1),SensorRange.axis(true,true));}
}
