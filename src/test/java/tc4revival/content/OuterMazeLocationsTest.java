package tc4revival.content;

import org.junit.jupiter.api.Test;
import tc4revival.debug.OuterMazeDebugCommands;
import static org.junit.jupiter.api.Assertions.*;

class OuterMazeLocationsTest {
    @Test void coordinatesFollowSavedPlanAcrossNegativeAndPositiveOrigins() {
        for(int cx:new int[]{-1234,0,987})for(long seed:new long[]{3,22,100}) {
            var plan=new EldritchMazeSavedData.Plan(cx,-cx,EldritchMazeLayout.generate(15,17,seed));
            var tablet=OuterMazeDebugCommands.locate(plan,false).orElseThrow();
            assertEquals(6,plan.layout().cell((tablet.getX()>>4)-plan.originX(),(tablet.getZ()>>4)-plan.originZ()).feature());
            assertEquals(53,tablet.getY());
            var boss=OuterMazeDebugCommands.locate(plan,true).orElseThrow();
            assertEquals(2,plan.layout().cell(((boss.getX()-16)>>4)-plan.originX(),((boss.getZ()-16)>>4)-plan.originZ()).feature());
            assertEquals(52,boss.getY());
        }
    }
}
