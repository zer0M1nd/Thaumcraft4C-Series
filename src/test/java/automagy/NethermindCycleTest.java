package automagy;
import automagy.logic.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NethermindCycleTest {
 @Test void reloadPreservesPaymentVariantAndOneTimeInstability(){var c=new NethermindCycle();c.select("blaze",true,2,Map.of("ignis",2));c.powerTick(true,n->20);c.acquire(id->true);assertTrue(c.checkInstabilityOnce());var resumed=new NethermindCycle();resumed.restore(c.snapshot());assertEquals(1,resumed.needed().get("ignis"));assertTrue(resumed.doubleBlaze());assertFalse(resumed.checkInstabilityOnce());assertFalse(resumed.advance());resumed.acquire(id->true);assertFalse(resumed.advance());assertTrue(resumed.advance());resumed.retry(10);assertEquals(10,resumed.remaining());assertTrue(resumed.needed().isEmpty());}
 @Test void exhaustedVisStopsPaidProcessingUntilPowerReturns(){var c=new NethermindCycle();c.select("rock",false,10,Map.of());assertFalse(c.powerTick(false,n->0));assertFalse(c.advance());assertEquals(10,c.remaining());assertTrue(c.powerTick(false,n->1));assertFalse(c.advance());assertFalse(c.powerTick(false,n->0));assertFalse(c.advance());assertEquals(9,c.remaining());}
 @Test void baitHalfPaymentSurvivesReloadWithoutDoubleCredit(){var b=new NetherBait();b.select("blaze",Map.of("fames",2),10);b.received("fames");var resumed=new NetherBait();resumed.restore(b.snapshot());assertEquals(1,resumed.needed().get("fames"));assertEquals(0,resumed.remaining("blaze"));resumed.received("fames");assertEquals(10,resumed.remaining("blaze"));assertFalse(resumed.received("fames"));assertEquals(10,resumed.remaining("blaze"));}
}
