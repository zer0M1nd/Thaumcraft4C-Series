package emtmodern.item;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneRingRulesTest {
    @Test void originalThresholdsAndRemovalBoundary(){
        assertEquals("warning",OneRingRules.effect(0,n->0));assertEquals("none",OneRingRules.effect(5999,n->0));
        assertEquals("nausea",OneRingRules.effect(6000,n->0));assertEquals("blindness",OneRingRules.effect(6001,n->0));
        assertEquals("magic",OneRingRules.effect(24000,n->0));assertEquals("launch",OneRingRules.effect(72000,n->0));assertEquals("wither",OneRingRules.effect(120000,n->0));
        assertFalse(OneRingRules.removable(600));assertTrue(OneRingRules.removable(601));
        assertEquals("death",OneRingRules.effect(Integer.MAX_VALUE-100,n->1));
    }
    @Test void blindnessAndNauseaUseSeparateRolls(){var calls=new java.util.ArrayList<Integer>();assertEquals("nausea",OneRingRules.effect(8000,n->{calls.add(n);return calls.size()==1?1:0;}));assertEquals(java.util.List.of(2000,2000),calls);}
}
