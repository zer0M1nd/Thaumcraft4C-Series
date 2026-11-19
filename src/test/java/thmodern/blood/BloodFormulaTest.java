package thmodern.blood;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodFormulaTest {
    private final BloodFormula formula=BloodFormula.bundled();
    @Test void preservesFortyEightAspectWeightTable(){assertEquals(48,formula.aspects().size());assertEquals(8,formula.maximum);}
    @Test void oneAirSelectsJumpWithLegacyWeightAndDuration(){
        var result=formula.calculate(Map.of("aer",1),a->0xff00ff);
        assertEquals(List.of(new BloodFormula.Effect("minecraft:jump_boost",400,0,4)),result.effects());assertEquals(0xff00ff,result.color());
    }
    @Test void exhaustedCandidatePoolDoesNotAppendInvalidEffectZero(){
        var result=formula.calculate(Map.of("perfodio",8),a->0xffffff);
        assertEquals(List.of(new BloodFormula.Effect("minecraft:haste",8000,2,80)),result.effects());
    }
    @Test void mixtureUsesWeightedColorAndDeterministicStrengthOrder(){
        var result=formula.calculate(Map.of("aer",1,"victus",3),a->a.equals("aer")?0xff0000:0x0000ff);
        assertEquals(0x3f00bf,result.color());assertEquals("minecraft:regeneration",result.effects().get(0).id());assertEquals(1800,result.effects().get(0).duration());assertEquals(2,result.effects().size());
    }
    @Test void invalidAndOverLimitSelectionsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->formula.calculate(Map.of("aer",9),a->0));
        assertThrows(IllegalArgumentException.class,()->formula.calculate(Map.of("aer",5,"terra",4),a->0));
        assertThrows(IllegalArgumentException.class,()->formula.calculate(Map.of("aer",-1),a->0));
        assertThrows(IllegalArgumentException.class,()->formula.calculate(Map.of("unknown",1),a->0));
        assertTrue(formula.calculate(Map.of(),a->0).effects().isEmpty());
    }
    @Test void instantHealingHasOneTickDuration(){
        var result=formula.calculate(Map.of("sano",8),a->0);
        assertEquals("minecraft:instant_health",result.effects().get(0).id());assertEquals(1,result.effects().get(0).duration());assertEquals(1,result.effects().get(0).amplifier());
    }
}
