package automagy;
import automagy.logic.NetherBait;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NetherBaitTest {
 @Test void partialConsumptionCannotAwardBaitTwice(){var b=new NetherBait();b.select("blaze",Map.of("fames",2,"victus",1),10);assertTrue(b.received("fames"));assertEquals(0,b.remaining("blaze"));b.received("fames");b.received("victus");assertEquals(10,b.remaining("blaze"));assertFalse(b.received("victus"));assertEquals(10,b.remaining("blaze"));assertTrue(b.consume());assertEquals(9,b.remaining("blaze"));b.select("blaze",Map.of("fames",2),10);assertTrue(b.needed().isEmpty());assertEquals(9,b.remaining("blaze"));}
 @Test void switchingFamiliesPreservesCompletedBaitOnly(){var b=new NetherBait();b.restore(Map.of("blaze",5,"ghast",0));b.select("ghast",Map.of("fames",6),6);b.received("fames");b.select("blaze",Map.of("fames",10),10);assertEquals(5,b.remaining("blaze"));assertTrue(b.needed().isEmpty());b.clearActive();assertFalse(b.consume());assertEquals(5,b.stored().get("blaze"));}
}
