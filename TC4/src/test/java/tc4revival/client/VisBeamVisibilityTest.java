package tc4revival.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VisBeamVisibilityTest {
    @Test void restingAndPulsingBeamsMatchLegacyRevealerMultipliers(){
        for(float opacity:new float[]{.3f,.8f}){
            assertEquals(opacity,VisBeamVisibility.ribbon(opacity,true),1e-6);
            assertEquals(opacity,VisBeamVisibility.flare(opacity,true),1e-6);
            assertEquals(VisBeamVisibility.ribbon(opacity,false)*10,VisBeamVisibility.ribbon(opacity,true),1e-6);
            assertEquals(VisBeamVisibility.flare(opacity,false)*5,VisBeamVisibility.flare(opacity,true),1e-6);
        }
    }
}
