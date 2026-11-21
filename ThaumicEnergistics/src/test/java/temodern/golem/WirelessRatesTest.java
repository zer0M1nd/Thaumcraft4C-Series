package temodern.golem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WirelessRatesTest {
    @Test void legacyUpgradeBudgets(){
        assertEquals(new WirelessRates(8,100,4),WirelessRates.of(0,false));
        assertEquals(new WirelessRates(24,250,12),WirelessRates.of(1,false));
        assertEquals(new WirelessRates(32,500,16),WirelessRates.of(2,false));
    }
    @Test void advancedDoublesAllChannels(){assertEquals(new WirelessRates(48,500,24),WirelessRates.of(1,true));}
    @Test void malformedUpgradeCountsRemainBounded(){assertEquals(WirelessRates.of(0,false),WirelessRates.of(-1,false));assertEquals(new WirelessRates(64,1000,32),WirelessRates.of(Integer.MAX_VALUE,true));}
}
