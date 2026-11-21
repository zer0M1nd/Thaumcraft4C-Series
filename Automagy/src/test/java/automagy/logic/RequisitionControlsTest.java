package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequisitionControlsTest {
    @Test void legacyModifiersAndMouseSigns(){
        assertEquals(1,RequisitionControls.delta(0,false,false,64));
        assertEquals(-1,RequisitionControls.delta(1,false,false,64));
        assertEquals(10,RequisitionControls.delta(0,false,true,64));
        assertEquals(-10,RequisitionControls.delta(1,false,true,64));
        assertEquals(16,RequisitionControls.delta(0,true,true,16));
        assertEquals(-1,RequisitionControls.delta(1,true,false,1));
        assertEquals(0,RequisitionControls.delta(2,false,false,64));
    }
    @Test void stockLimitAndExplicitUnavailableRequests(){
        assertEquals(2,RequisitionControls.limit(64,false,false,32,30));
        assertEquals(0,RequisitionControls.limit(1,false,false,32,40));
        assertEquals(64,RequisitionControls.limit(64,false,true,32,40));
        assertEquals(64,RequisitionControls.limit(64,true,false,0,40));
        assertEquals(-3,RequisitionControls.limit(-64,false,false,0,3));
        assertEquals(0,RequisitionControls.limit(-10,true,false,0,0));
    }
}
