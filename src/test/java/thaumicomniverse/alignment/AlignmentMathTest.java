package thaumicomniverse.alignment;

import org.junit.jupiter.api.Test;


import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AlignmentMathTest {
    @Test void targetFingerprintInputIsIndependentOfMapIterationOrder() {
        var forward=new LinkedHashMap<String,Long>();forward.put("aer",1L);forward.put("terra",2L);
        var reverse=new LinkedHashMap<String,Long>();reverse.put("terra",2L);reverse.put("aer",1L);
        var first=new AlignmentMath.Offering("sample",2,-1,forward);
        var second=new AlignmentMath.Offering("sample",2,-1,reverse);
        assertEquals(first.toString(),second.toString());
        assertEquals("{aer=1, terra=2}",first.aspects().toString());
        assertEquals(AlignmentMath.summarize(List.of(first)).toString(),AlignmentMath.summarize(List.of(second)).toString());
    }
    @Test void signedOfferingsMustMatchEveryAspectAndCountAllConsumedItems() {
        var actual=AlignmentMath.summarize(List.of(
                new AlignmentMath.Offering("netherrack",2,1,Map.of("ignis",1L,"terra",1L,"infernus",1L)),
                new AlignmentMath.Offering("crystal{infernus}",1,-1,Map.of("infernus",2L))));
        assertTrue(AlignmentMath.matches(actual,Map.of("ignis",2L,"terra",2L)));
        assertFalse(AlignmentMath.matches(actual,Map.of("ignis",4L)));
        assertEquals(3,actual.count()); assertEquals(2,actual.kinds());
        var opposite=AlignmentMath.summarize(List.of(
                new AlignmentMath.Offering("same",64,1,Map.of("aer",1L)),
                new AlignmentMath.Offering("same",63,-1,Map.of("aer",1L))));
        assertEquals(127,opposite.count()); assertEquals(1,opposite.kinds());
        assertEquals(Map.of("aer",1L),opposite.vector());
    }
    @Test void efficiencyThresholdsAreExactAndYieldNeverDropsBelowTheFloor() {
        assertEquals(0,AlignmentMath.penaltySteps(7,2,4,2));
        assertEquals(1,AlignmentMath.penaltySteps(8,2,4,2));
        assertEquals(1,AlignmentMath.penaltySteps(4,3,4,2));
        assertEquals(4,AlignmentMath.penaltySteps(16,5,4,2));
        assertEquals(List.of(16,12,9,6,4,3,2,1,1),java.util.stream.IntStream.range(0,9)
                .map(i->AlignmentMath.yield(1,16,i)).boxed().toList());
        assertEquals(3,AlignmentMath.yield(3,16,500));
        assertEquals(0,AlignmentMath.penaltySteps(1,1,100,50));
        assertThrows(ArithmeticException.class,()->AlignmentMath.summarize(List.of(
                new AlignmentMath.Offering("huge",2,1,Map.of("aer",Long.MAX_VALUE)))));
    }
}
