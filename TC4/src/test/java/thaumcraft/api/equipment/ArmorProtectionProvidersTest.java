package thaumcraft.api.equipment;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.api.equipment.ArmorProtectionProviders.*;

class ArmorProtectionProvidersTest {
    @Test void mixedFamiliesSettleTogetherWithOneWearEntryPerSlot(){
        var result=ArmorProtectionProviders.settle(20,List.of(
            new SlotProtection(0,new Protection(0,.1,1000,false,false)),
            new SlotProtection(1,new Protection(1,.25,1000,true,false)),
            new SlotProtection(2,new Protection(1,.25,1000,false,true))));
        assertEquals(9,result.remaining(),.0001);
        assertEquals(Map.of(0,1,1,5,2,5),result.wear());
    }
    @Test void providersCannotSettleTheSameSlotTwice(){
        var piece=new SlotProtection(0,new Protection(0,.1,1000,false,false));
        assertThrows(IllegalArgumentException.class,()->ArmorProtectionProviders.settle(20,List.of(piece,piece)));
        assertThrows(IllegalArgumentException.class,()->new Protection(0,Double.NaN,100,false,false));
    }
}
