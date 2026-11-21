package thmodern.plane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class RiftLifetimeTest {
 @Test void sixBeamsFreezeInstabilityButDoNotResetIt(){var life=new RiftLifetime();for(int i=0;i<50;i++)assertFalse(life.tick(0,false));assertTrue(life.ready());life.tick(0,false);assertEquals(56,life.count());for(int i=0;i<1000;i++)life.tick(6,false);assertEquals(56,life.count());life.tick(5,false);assertEquals(57,life.count());}
 @Test void instabilityThresholdAndCollapseDelayRemainDistinct(){var life=new RiftLifetime(2400,false);assertFalse(life.tick(5,false));assertTrue(life.collapsing());assertEquals(0,life.count());for(int i=0;i<25;i++)assertFalse(life.tick(6,false));assertTrue(life.tick(6,false));var stable=new RiftLifetime();for(int i=0;i<10000;i++)assertFalse(stable.tick(0,true));assertEquals(50,stable.count());assertFalse(stable.collapsing());}
}
