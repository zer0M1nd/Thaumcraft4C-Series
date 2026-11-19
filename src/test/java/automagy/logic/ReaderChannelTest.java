package automagy.logic;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReaderChannelTest {
    private final AspectReading node=new AspectReading(Map.of("aer",20,"ignis",5),Map.of("aer",100,"ignis",10),110,0,true,true);
    @Test void nodeExtremesCompareFractionsRatherThanRawVisAmounts(){
        assertEquals(8,new ReaderChannel(2,0,6,false).evaluate(node,""));
        assertEquals(3,new ReaderChannel(3,0,6,false).evaluate(node,""));
        assertEquals(4,new ReaderChannel(1,0,6,false).evaluate(node,""));
    }
    @Test void labelSelectsItsOwnNodeCapacityAndManualCapacityOverridesIt(){
        assertEquals(8,new ReaderChannel(1,0,6,false).evaluate(node,"ignis"));
        assertEquals(2,new ReaderChannel(1,64,6,false).evaluate(node,"ignis"));
        assertEquals(0,new ReaderChannel(1,0,6,false).evaluate(node,"aqua"));
    }
    @Test void missingTargetDoesNotProduceInvertedPowerButAnEmptyVesselDoes(){
        var channel=new ReaderChannel(1,0,6,true);
        assertEquals(0,channel.evaluate(AspectReading.MISSING,""));
        assertEquals(15,channel.evaluate(new AspectReading(Map.of(),Map.of(),64,64,false,true),""));
    }
    @Test void countedTypesRetainAmplifiedOutputAndClampBeforeInversion(){
        var contents=new HashMap<String,Integer>();for(int i=0;i<30;i++)contents.put("aspect"+i,1);
        var reading=new AspectReading(contents,Map.of(),64,64,false,true);
        assertEquals(30,new ReaderChannel(4,0,6,false).evaluate(reading,"ignored"));
        assertEquals(0,new ReaderChannel(4,0,6,true).evaluate(reading,""));
    }
    @Test void zeroAndSaturatedProportionsAndInvalidDirectionsAreHandled(){
        assertEquals(0,ReaderChannel.proportion(0,64));assertEquals(15,ReaderChannel.proportion(64,64));
        assertEquals(1,ReaderChannel.proportion(1,125));assertEquals(0,ReaderChannel.proportion(1,0));
        assertThrows(IllegalArgumentException.class,()->new ReaderChannel(1,0,1,false));
    }
}
