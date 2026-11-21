package thmodern.boat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class BoatMotionTest {
    @Test void modernSteeringAndReverseAreNotReplacedByLegacyMotion(){
        assertEquals(new BoatMotion.Horizontal(.02,-.03),BoatMotion.row(false,.02,-.03,90,false));
        var east=BoatMotion.row(false,0,0,-90,true);assertTrue(east.x()>0);assertEquals(0,east.z(),1e-8);
    }
    @Test void bothMaterialsRetainSpeedLimitAndMetalAcceleratesFaster(){
        for(boolean metal:new boolean[]{false,true}){
            var boosted=BoatMotion.row(metal,4,4,0,true);assertEquals(.45,Math.hypot(boosted.x(),boosted.z()),1e-9);
        }
        assertTrue(BoatMotion.row(true,0,.04,0,true).z()>BoatMotion.row(false,0,.04,0,true).z());
    }
}
