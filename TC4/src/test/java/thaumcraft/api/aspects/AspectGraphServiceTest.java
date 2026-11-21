package thaumcraft.api.aspects;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectGraphServiceTest {
    private static AspectGraphService graph(){return new AspectGraphService(Map.of(
        "air",List.of(),"fire",List.of(),"steam",List.of("air","fire"),
        "loop",List.of("loop","steam"),"double",List.of("loop","loop")));}
    @Test void cyclicPathsTerminateAndPreserveRepeatedChildren(){
        assertEquals(Map.of("air",2L,"fire",2L),graph().reduce(Map.of("double",1L),AspectGraphService.Reduction.SUM));
        assertEquals(Map.of("air",2L,"fire",2L),graph().reduce(Map.of("double",1L),AspectGraphService.Reduction.MAXIMUM));
    }
    @Test void maximumCombinesSeparateInputsWithoutSummingThem(){
        assertEquals(Map.of("air",3L,"fire",3L),graph().reduce(Map.of("loop",3L,"steam",2L),AspectGraphService.Reduction.MAXIMUM));
    }
    @Test void weightsAreCappedPerCompoundAndNotPerPrimal(){
        var g=new AspectGraphService(Map.of("a",List.of(),"b",List.of(),"x",List.of("a","b"),"y",List.of("x","a")));
        assertEquals(10,g.weightedFold("y",id->id.equals("a")?9:1,10));
        assertEquals(9,g.weightedFold("a",id->9,5));
        assertEquals(19,g.weightedFold("y",id->id.equals("a")?9:1,-1));
        assertEquals(2,graph().weightedFold("loop",id->1,-1));
    }
    @Test void invalidReferencesAndOverflowFailExplicitly(){
        assertThrows(IllegalArgumentException.class,()->new AspectGraphService(Map.of("bad",List.of("bad","missing"))));
        assertThrows(ArithmeticException.class,()->graph().reduce(Map.of("double",Long.MAX_VALUE),AspectGraphService.Reduction.SUM));
        assertThrows(IllegalArgumentException.class,()->graph().weightedFold("air",id->Double.NaN,-1));
    }
}
