package automagy;
import automagy.logic.ExperienceBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceBufferTest {
 @Test void transferCapsAndSimulationPreserveStoredExperience(){var b=new ExperienceBuffer(2000);assertEquals(10,b.insert(100,10,true));assertEquals(0,b.stored());assertEquals(10,b.insert(100,10,false));assertEquals(3,b.extract(3,10,true));assertEquals(10,b.stored());assertEquals(10,b.extract(100,10,false));assertEquals(0,b.stored());}
 @Test void storageBoundsAndComparatorAreSafeAtIntegerLimits(){var b=new ExperienceBuffer(2000);b.restore(Integer.MAX_VALUE);assertEquals(15,b.comparator());assertEquals(0,b.insert(Integer.MAX_VALUE,Integer.MAX_VALUE,false));b.restore(-1);assertEquals(0,b.comparator());b.insert(1,10,false);assertEquals(1,b.comparator());}
 @Test void balanceRetainsOddUnitDirection(){assertEquals(0,ExperienceBuffer.balanceWithBrain(50,50,2000));assertEquals(-50,ExperienceBuffer.balanceWithBrain(100,0,2000));assertEquals(50,ExperienceBuffer.balanceWithBrain(0,100,2000));assertEquals(1,ExperienceBuffer.balanceWithBrain(2,3,2000));}
}
