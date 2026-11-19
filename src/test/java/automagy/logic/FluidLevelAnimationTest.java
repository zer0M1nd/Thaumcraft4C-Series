package automagy.logic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FluidLevelAnimationTest {
 @Test void fillingAndDrainingDoNotJumpOrOvershoot(){var a=new FluidLevelAnimation();assertEquals(0,a.sample(0,0));float fill=a.sample(1,1);assertTrue(fill>0&&fill<1);float drain=a.sample(2,0);assertTrue(drain>0&&drain<fill);}
 @Test void differentFrameRatesReachSameHeight(){var a=new FluidLevelAnimation();var b=new FluidLevelAnimation();a.sample(0,0);b.sample(0,0);for(int i=1;i<20;i++)b.sample(i/20.,1);assertEquals(a.sample(1,1),b.sample(1,1),.00001);}
 @Test void firstFrameAndWorldTimeResetUseActualContents(){var a=new FluidLevelAnimation();assertEquals(.8f,a.sample(100,.8f));assertEquals(.3f,a.sample(0,.3f));}
}
