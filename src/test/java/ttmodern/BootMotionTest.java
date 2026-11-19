package ttmodern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BootMotionTest {
    @Test void ordinaryBootsBoostForwardOnly(){assertEquals(new BootMotion.Input(0,0),BootMotion.boosted(-1,1,false));assertEquals(new BootMotion.Input(.5f,0),BootMotion.boosted(.5f,-1,false));}
    @Test void omnidirectionalBootsPreserveBackwardAndStrafeInputs(){assertEquals(new BootMotion.Input(-1,1),BootMotion.boosted(-1,1,true));assertEquals(new BootMotion.Input(0,-1),BootMotion.boosted(0,-1,true));}
    @Test void invalidExternalSettingsCannotCreateNonfiniteMovement(){assertEquals(new BootMotion.Input(0,0),BootMotion.boosted(Float.NaN,Float.POSITIVE_INFINITY,true));assertEquals(1,BootMotion.multiplier(Double.NaN));assertEquals(1,BootMotion.multiplier(Double.NEGATIVE_INFINITY));assertEquals(0,BootMotion.multiplier(-1));assertEquals(10,BootMotion.multiplier(99));}
}
