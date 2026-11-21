package emtmodern.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuantumFlightRulesTest {
    @Test void worldCeilingOnlyReducesLastTwentyFiveBlocks(){
        assertEquals(1,QuantumFlightRules.jetpackPower(2000000,295,320));
        assertEquals(.5,QuantumFlightRules.jetpackPower(2000000,307.5,320),.00001);
        assertEquals(0,QuantumFlightRules.jetpackPower(2000000,320,320));
        assertEquals(0,QuantumFlightRules.jetpackPower(2000000,400,320));
    }
    @Test void legacyLowChargeThresholdAndHoverMomentum(){
        assertEquals(1,QuantumFlightRules.jetpackPower(1501,0,320));
        assertEquals(.0025,QuantumFlightRules.jetpackPower(1500,0,320),.00001);
        assertEquals(.1,QuantumFlightRules.jetpackVertical(-.01,1,true,true,true),.00001);
        assertEquals(-.1,QuantumFlightRules.jetpackVertical(-.2,1,true,false,true),.00001);
        assertEquals(.4,QuantumFlightRules.jetpackVertical(.4,1,true,false,false),.00001);
        assertEquals(.6,QuantumFlightRules.jetpackVertical(1,1,false,false,false),.00001);
    }
}
