package thaumicfutures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumicfutures.foci.BuilderLayout;
class BuilderLayoutTest {
    @Test void originalEndpointExcludesClickedBlockAndPlayerCoordinate() {
        assertEquals(8,BuilderLayout.length(.5,10,0));
        assertEquals(8,BuilderLayout.length(10.5,1,0));
        assertEquals(0,BuilderLayout.length(2.5,2,0));
        assertEquals(0,BuilderLayout.length(2.5,3.49,0));
    }
    @Test void tc5BaseAndEnlargeCapsArePreserved() {
        assertEquals(15,BuilderLayout.length(.5,100,0));
        assertEquals(23,BuilderLayout.length(.5,100,1));
        assertEquals(55,BuilderLayout.length(.5,100,5));
        assertEquals(15,BuilderLayout.length(.5,100,-1));
        assertEquals(55,BuilderLayout.length(.5,100,Integer.MAX_VALUE));
    }
    @Test void negativeCoordinatesAndNonfinitePositionsAreSafe() {
        assertEquals(8,BuilderLayout.length(-.5,-10,0));
        assertEquals(8,BuilderLayout.length(-10.5,-1,0));
        assertEquals(0,BuilderLayout.length(Double.NaN,10,0));
        assertEquals(0,BuilderLayout.length(0,Double.POSITIVE_INFINITY,5));
    }
}
