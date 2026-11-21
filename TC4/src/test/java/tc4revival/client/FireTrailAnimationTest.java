package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FireTrailAnimationTest {
    @Test void allFiveLegacyLifetimesUseProportionalFrames() {
        int[][] expected={{0,1,2,3,5,6,7},{0,1,2,3,4,5,6,7},{0,1,2,3,4,5,6,7,8},{0,0,1,2,3,4,5,6,7,8},{0,0,1,2,3,4,4,5,6,7,8}};
        for(int i=0;i<expected.length;i++)for(int age=0;age<expected[i].length;age++)assertEquals(expected[i][age],FireTrailAnimation.frame(age,7+i),"lifetime="+(7+i)+" age="+age);
    }
    @Test void firstAndLastFramesHaveHalfAlpha() {
        for(int lifetime=7;lifetime<=11;lifetime++) {
            assertEquals(.4f,FireTrailAnimation.alpha(0,lifetime));assertEquals(.4f,FireTrailAnimation.alpha(1,lifetime));
            assertEquals(.8f,FireTrailAnimation.alpha(2,lifetime));assertEquals(.8f,FireTrailAnimation.alpha(lifetime-2,lifetime));
            assertEquals(.4f,FireTrailAnimation.alpha(lifetime-1,lifetime));assertEquals(8,FireTrailAnimation.frame(lifetime,lifetime));
        }
    }
}
