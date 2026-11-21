package automagy.logic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TeleportLandingTest {
 @Test void retainsFractionalHeightWhileDescending(){assertEquals(64.75,TeleportLanding.find(80.75,-64,y->y==63,y->true).orElseThrow());}
 @Test void supportsModernNegativeWorldHeights(){assertEquals(-62.5,TeleportLanding.find(-40.5,-64,y->y==-64,y->true).orElseThrow());}
 @Test void noFloorAndBlockedOrWetLandingReject(){assertTrue(TeleportLanding.find(20,-64,y->false,y->true).isEmpty());assertTrue(TeleportLanding.find(20,-64,y->y==10,y->false).isEmpty());}
 @Test void neverSearchesThroughFirstSolidFloor(){assertTrue(TeleportLanding.find(20,-64,y->y==15||y==10,y->y==11).isEmpty());}
 @Test void rejectsInvalidCoordinates(){assertTrue(TeleportLanding.find(Double.NaN,-64,y->true,y->true).isEmpty());assertTrue(TeleportLanding.find(-65,-64,y->true,y->true).isEmpty());}
}
