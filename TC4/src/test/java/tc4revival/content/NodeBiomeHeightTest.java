package tc4revival.content;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NodeBiomeHeightTest {
    @Test void surfaceAndCaveEffectsCannotCrossTerrainBoundary(){
        for(int y=-16;y<80;y++) {
            assertEquals(y>=16,WorldBiomeChanges.nodeQuartAllowed(true,20,16,y));
            assertEquals(y<16&&Math.abs(y-8)<=2,WorldBiomeChanges.nodeQuartAllowed(false,8,16,y));
        }
    }
    @Test void negativeYAndNearSurfaceCavesStayLocal(){
        assertTrue(WorldBiomeChanges.nodeQuartAllowed(false,-8,16,-10));
        assertFalse(WorldBiomeChanges.nodeQuartAllowed(false,-8,16,-11));
        assertTrue(WorldBiomeChanges.nodeQuartAllowed(false,15,16,15));
        assertFalse(WorldBiomeChanges.nodeQuartAllowed(false,15,16,16));
    }
}
