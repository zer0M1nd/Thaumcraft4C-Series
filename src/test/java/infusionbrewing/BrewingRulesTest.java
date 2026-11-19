package infusionbrewing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class BrewingRulesTest {
    @ParameterizedTest @CsvSource({"1,4800","2400,4800","2401,9600","4800,9600","4801,19200","9600,19200","9601,-1","19200,-1","0,-1","2147483647,-1"})
    void durationUsesOriginalThresholdsWithoutOverflow(int input,int output){assertEquals(output,BrewingRules.nextDuration(input));}
    @Test void legacyClassificationIsComplete(){int good=0,instant=0,noAmp=0;for(int i=1;i<=23;i++){if(BrewingRules.support(i))good++;if(BrewingRules.instant(i))instant++;if(BrewingRules.noAmplifier(i))noAmp++;}assertEquals(14,good);assertEquals(2,instant);assertEquals(6,noAmp);}
    @ParameterizedTest @CsvSource({"0,1200","1,600","2,400","3,300"})
    void upkeepScalesWithStrength(int amplifier,int ticks){assertEquals(ticks,BrewingRules.upkeepInterval(amplifier));}
    @ParameterizedTest @CsvSource({"0,6,4,200,800","1,12,6,160,320","2,16,8,80,160","3,20,10,40,80","4,24,12,20,40"})
    void spawnerTiersMatchOriginalValues(int tier,int nearby,int count,int min,int max){assertEquals(nearby,BrewingRules.nearby(tier));assertEquals(count,BrewingRules.spawnCount(tier));assertEquals(min,BrewingRules.minDelay(tier));assertEquals(max,BrewingRules.maxDelay(tier));assertEquals(tier,BrewingRules.powerTier(nearby,count));assertEquals(tier,BrewingRules.speedTier(min,max));}
    @Test void unevenSpawnerUsesLesserTier(){assertEquals(1,BrewingRules.powerTier(24,6));assertEquals(1,BrewingRules.speedTier(160,40));}
}
