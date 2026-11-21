package fmmodern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SinRulesTest {
    @Test void netherShardProbabilitiesMatchLegacyThresholds(){
        assertEquals(1,java.util.stream.IntStream.range(0,SinRules.ENVY_ROLL_BOUND).filter(SinRules::envyCarrier).count());
        assertEquals(4,java.util.stream.IntStream.range(0,SinRules.LUST_ROLL_BOUND).filter(SinRules::cropLust).count());
        assertTrue(SinRules.envyCarrier(1));assertFalse(SinRules.envyCarrier(0));
        assertTrue(SinRules.cropLust(3));assertFalse(SinRules.cropLust(4));
    }
    @Test void legacyDropThresholdsRemainInclusive(){assertTrue(SinRules.silverfish(2,0,true));assertFalse(SinRules.silverfish(3,0,true));assertTrue(SinRules.silverfish(7,2,false));assertFalse(SinRules.silverfish(8,2,false));}
    @Test void educationalAddsThreeBaseRewardsPerLevel(){assertEquals(5,SinRules.educatedExperience(5,0));assertEquals(20,SinRules.educatedExperience(5,1));assertEquals(80,SinRules.educatedExperience(5,5));assertEquals(0,SinRules.educatedExperience(0,5));}
    @Test void skullDropChancesDistinguishTargets(){assertTrue(SinRules.skull(3,0,"skeleton"));assertFalse(SinRules.skull(3,0,"zombie"));assertTrue(SinRules.skull(6,2,"creeper"));assertFalse(SinRules.skull(6,2,"player"));}

    @org.junit.jupiter.api.Test void corruptionRespectsLustToggleWithoutBias(){
        var enabled=new java.util.HashSet<Integer>();var disabled=new java.util.HashSet<Integer>();
        for(int i=0;i<7;i++)enabled.add(SinRules.corruptionSin(i,false));
        for(int i=0;i<6;i++)disabled.add(SinRules.corruptionSin(i,true));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(0,1,2,3,4,5,6),enabled);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(0,1,2,3,5,6),disabled);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->SinRules.corruptionSin(6,true));
    }
}
