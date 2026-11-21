package thmodern.lens;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class LensScanProgressTest {
    @Test void repeatedFramesCannotAccelerateGameTime(){var timer=new LensScanProgress();for(int i=0;i<1000;i++)assertFalse(timer.observe("block",0));for(int i=1;i<80;i++)assertFalse(timer.observe("block",i));assertTrue(timer.observe("block",80));assertEquals(0,timer.progress());}
    @Test void lostTicksAndChangedTargetsRestartObservation(){var timer=new LensScanProgress();timer.observe("a",1);timer.observe("a",2);assertEquals(1,timer.progress());timer.observe("a",4);assertEquals(0,timer.progress());timer.observe("b",5);assertEquals(0,timer.progress());timer.observe(null,6);assertEquals(0,timer.progress());assertFalse(timer.observe("b",1000));assertFalse(timer.observe("b",999));assertEquals(0,timer.progress());}
}
