package automagy.logic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfinityJarBufferTest {
 @Test void lastUnitAndRefillingWithdrawalsAreReplaced(){var jar=new InfinityJarBuffer();jar.select("aer");assertTrue(jar.extract("aer",1));assertEquals(1,jar.amount());jar.tick(true);assertEquals(2,jar.amount());assertTrue(jar.extract("aer",1));assertEquals(2,jar.amount());assertFalse(jar.extract("ignis",1));}
 @Test void fullJarDrainsUntilBelowHalfThenRefills(){var jar=new InfinityJarBuffer();jar.insert("aer",64);assertTrue(jar.extract("aer",33));assertEquals(31,jar.amount());jar.tick(false);assertEquals(31,jar.amount());for(int i=0;i<40;i++)jar.tick(true);assertEquals(64,jar.amount());}
 @Test void voidOverflowAndTypeSeparation(){var jar=new InfinityJarBuffer();assertEquals(0,jar.insert("aer",100));assertEquals(64,jar.amount());assertEquals(10,jar.insert("ignis",10));var copy=new InfinityJarBuffer();copy.restore(jar.snapshot());assertEquals(jar.snapshot(),copy.snapshot());}
}
