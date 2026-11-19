package ttmodern;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CropTraitsTest {
    @Test void malformedTraitsCannotMakeGrowthRandomBoundsInvalid() {
        for(int value:new int[]{Integer.MIN_VALUE,-1,0,5,49,50,51,Integer.MAX_VALUE}) {
            assertTrue(CropTraits.growthBound(value)>=60);assertTrue(CropTraits.exchangeBound(value)>=500);
        }
        assertEquals(15060,CropTraits.growthBound(0));assertEquals(60,CropTraits.growthBound(50));
    }
    @Test void saturatedTraitsAreBoundedAndZerosRemoved() {
        var traits=new LinkedHashMap<String,Integer>();for(String key:CropTraits.PRIMALS)traits.put(key,Integer.MAX_VALUE);traits.put("bad",-1);
        CropTraits.cap(traits,50,bound->bound-1);assertEquals(50,traits.values().stream().mapToInt(Integer::intValue).sum());assertFalse(traits.containsKey("bad"));assertTrue(traits.values().stream().allMatch(v->v>0));
    }
    @Test void unsaturatedTraitsKeepTheirDistribution() {
        var traits=new LinkedHashMap<>(Map.of("aer",3,"terra",5));var before=Map.copyOf(traits);CropTraits.cap(traits,50,bound->{fail("No randomness needed below cap");return 0;});assertEquals(before,traits);
    }
}
