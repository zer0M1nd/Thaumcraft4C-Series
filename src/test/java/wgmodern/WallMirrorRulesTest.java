package wgmodern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WallMirrorRulesTest {
    @Test void projectionOriginMatchesLegacyMirrorAndPlayerRendererTogether(){
        for(boolean local:new boolean[]{true,false})for(boolean crouching:new boolean[]{true,false})for(double feet:new double[]{-7,-6.5,20}){
            double offset=local?1.62f:0;
            double legacyPosition=feet+offset;
            double legacyMirrorOffset=legacyPosition-offset-(-7);
            double rendererOrigin=legacyMirrorOffset+offset/2-offset-(local?0:.75)-(crouching&&!local?.125:0);
            assertEquals(rendererOrigin,WallMirrorRules.reflectionOriginY(feet,-7,local,crouching),.00001);
        }
    }
    @Test void openingTakesThirtyTwoTicksAndIdleFrameDoesNotAnimate(){var s=new WallMirrorRules.Step(0,false,true,false);for(int i=0;i<31;i++)s=WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating());assertEquals(31,s.frame());assertFalse(s.active());s=WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating());assertTrue(s.active());assertFalse(s.activating());assertEquals(s,WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating()));}
    @Test void closingKeepsOriginalTwoSpeedFrameSchedule(){var s=new WallMirrorRules.Step(31,true,false,true);for(int i=0;i<15;i++)s=WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating());assertEquals(16,s.frame());for(int i=0;i<8;i++)s=WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating());assertEquals(0,s.frame());assertTrue(s.active());s=WallMirrorRules.advance(s.frame(),s.active(),s.activating(),s.deactivating());assertFalse(s.active());assertFalse(s.deactivating());}
    @Test void playerProjectionShrinksContinuouslyAcrossTheOriginalNineBlockRange(){assertEquals(.8125f,WallMirrorRules.reflectionScale(0));assertEquals(.40625f,WallMirrorRules.reflectionScale(4.5));assertEquals(0,WallMirrorRules.reflectionScale(9));}
}
