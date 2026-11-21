package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PearlRulesTest {
    @Test void baseFractureThresholdAndDiscoveryEscalation(){
        assertTrue(PearlRules.fractures(1,0,7));assertFalse(PearlRules.fractures(1,0,8));
        assertTrue(PearlRules.fractures(1,1,17));assertFalse(PearlRules.fractures(1,1,18));
        assertTrue(PearlRules.fractures(1,10,100));assertFalse(PearlRules.fractures(0,10,1));
    }
    @Test void goldUsesItemCountRatherThanEntityCount(){
        assertFalse(PearlRules.goldBonus(150,100));assertTrue(PearlRules.goldBonus(1,1));
        assertFalse(PearlRules.goldBonus(3,2));assertTrue(PearlRules.goldBonus(3,3));
    }
    @Test void miserLimitIsCumulativeAcrossFlightAndImpact(){
        assertTrue(PearlRules.canCollect(true,9));assertFalse(PearlRules.canCollect(true,10));
        assertTrue(PearlRules.canCollect(false,100));
    }
}
