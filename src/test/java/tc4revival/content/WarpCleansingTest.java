package tc4revival.content;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.research.PlayerKnowledge;
import static org.junit.jupiter.api.Assertions.*;

class WarpCleansingTest {
    private PlayerKnowledge knowledge() {
        var result=new PlayerKnowledge(new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList()));
        result.setWarp(PlayerKnowledge.WarpKind.PERMANENT,40);
        result.setWarp(PlayerKnowledge.WarpKind.STICKY,2);
        result.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,9);
        result.setWarpCounter(1);
        return result;
    }
    @Test void bonusesUseStrictProbabilityBoundaryAndOnlyRemoveOneStickyWarp() {
        for (boolean warded:new boolean[]{false,true}) for (boolean water:new boolean[]{false,true}) {
            float threshold=.33f+(warded?.25f:0)+(water?.25f:0);
            var failure=knowledge();
            assertFalse(WarpCleansing.wash(failure,warded,water,threshold));
            assertEquals(2,failure.warp(PlayerKnowledge.WarpKind.STICKY));
            assertEquals(0,failure.warp(PlayerKnowledge.WarpKind.TEMPORARY));
            assertEquals(42,failure.warpCounter());
            var success=knowledge();
            assertTrue(WarpCleansing.wash(success,warded,water,Math.nextDown(threshold)));
            assertEquals(1,success.warp(PlayerKnowledge.WarpKind.STICKY));
            assertEquals(40,success.warp(PlayerKnowledge.WarpKind.PERMANENT));
            assertEquals(0,success.warp(PlayerKnowledge.WarpKind.TEMPORARY));
            assertEquals(41,success.warpCounter());
        }
    }
    @Test void noReductionDoesNotReactivateDormantEvents() {
        var state=knowledge(); state.setWarp(PlayerKnowledge.WarpKind.STICKY,0);
        state.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,0); state.setWarpCounter(-4);
        assertFalse(WarpCleansing.wash(state,true,true,0));
        assertEquals(-4,state.warpCounter()); assertEquals(40,state.warp(PlayerKnowledge.WarpKind.PERMANENT));
    }
    @Test void protectionUsesTruncatedSquareRootAndLegacyDurationCap() {
        assertEquals(32000,WarpCleansing.wardDuration(0));
        assertEquals(32000,WarpCleansing.wardDuration(36));
        assertEquals(20000,WarpCleansing.wardDuration(100));
        assertEquals(20000,WarpCleansing.wardDuration(120));
        assertEquals(10000,WarpCleansing.wardDuration(400));
    }
}
