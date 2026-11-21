package thaumcraft.api.wands;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WandPropertyLayersTest {
    @Test void explicitPriorityIsIndependentOfRegistrationOrder(){
        WandPropertyLayers.registerCapacity("test:infinity",100,Set.of("test:rod"),(rod,base)->OptionalInt.of(9_999_999));
        WandPropertyLayers.registerCapacity("test:progression",0,Set.of("test:rod","test:normal"),(rod,base)->OptionalInt.of(400));
        assertEquals(9_999_999,WandPropertyLayers.resolveCapacity("test:rod",100));
        assertEquals(400,WandPropertyLayers.resolveCapacity("test:normal",100));
        assertEquals(100,WandPropertyLayers.resolveCapacity("other:rod",100));
        assertDoesNotThrow(()->Math.multiplyExact(WandPropertyLayers.resolveCapacity("test:rod",100),150));
    }
    @Test void invalidCapacityCannotOverflowSceptreStorage(){
        assertEquals(14_316_557,WandPropertyLayers.MAX_CAPACITY);
        assertThrows(IllegalArgumentException.class,()->WandPropertyLayers.checkedCapacity(WandPropertyLayers.MAX_CAPACITY+1));
        assertThrows(IllegalArgumentException.class,()->WandPropertyLayers.checkedCapacity(0));
    }
}
