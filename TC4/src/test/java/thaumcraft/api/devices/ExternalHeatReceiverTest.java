package thaumcraft.api.devices;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalHeatReceiverTest {
    @Test void energyAndHeatBoundsAreRespected(){
        assertEquals(0,ExternalHeatReceiver.heatTicks(0,100,10,false));
        assertEquals(0,ExternalHeatReceiver.heatTicks(0,9,10,true));
        assertEquals(3,ExternalHeatReceiver.heatTicks(197,100,10,true));
        assertEquals(0,ExternalHeatReceiver.heatTicks(200,100,10,true));
        assertEquals(4,ExternalHeatReceiver.heatTicks(0,Integer.MAX_VALUE,1,true));
        assertEquals(1,ExternalHeatReceiver.heatTicks(0,Integer.MAX_VALUE,Integer.MAX_VALUE,true));
        assertEquals(4,ExternalHeatReceiver.heatTicks(0,4,0,true));
    }
}
