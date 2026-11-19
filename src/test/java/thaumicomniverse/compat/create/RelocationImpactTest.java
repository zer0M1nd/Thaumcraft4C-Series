package thaumicomniverse.compat.create;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelocationImpactTest {
    @Test void nonLinearMassAndSpeedRemainBoundedByTheNativeTrainHit(){
        for(double speed:new double[]{0,.1,.35,.5,1,2,10})for(double hardness:new double[]{0,.5,1.5,50})
            for(int blocks:new int[]{1,10,1000})for(boolean monster:new boolean[]{false,true}){
                float damage=RelocationImpact.damage(blocks*RelocationImpact.contribution(hardness),speed,monster);
                assertTrue(damage>=0&&Float.isFinite(damage));
                assertTrue(damage<=Math.floor(speed*(monster?32:16)));
                if(speed<=.35)assertEquals(0,damage);
            }
    }
    @Test void hardnessAndSpeedIncreaseDamageWithoutLinearBlockMultiplication(){
        double soft=RelocationImpact.contribution(0),hard=RelocationImpact.contribution(10);
        assertTrue(hard>soft);assertTrue(hard<soft*40);
        assertTrue(RelocationImpact.damage(soft,1,false)<RelocationImpact.damage(hard,1,false));
        assertTrue(RelocationImpact.damage(soft,.5,false)<RelocationImpact.damage(soft,1,false));
        assertTrue(RelocationImpact.damage(soft*2,2,false)<2*RelocationImpact.damage(soft,2,false));
    }
    @Test void invalidNumbersCannotBecomeInfiniteDamage(){
        assertEquals(0,RelocationImpact.damage(Double.NaN,1,false));
        assertEquals(0,RelocationImpact.damage(10,Double.POSITIVE_INFINITY,true));
    }
}
