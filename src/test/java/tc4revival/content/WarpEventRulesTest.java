package tc4revival.content;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.research.PlayerKnowledge;
import static org.junit.jupiter.api.Assertions.*;

class WarpEventRulesTest {
    private PlayerKnowledge knowledge() {
        return new PlayerKnowledge(new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList()));
    }
    private IntUnaryOperator rolls(int... values) {
        return new IntUnaryOperator() {
            int index;
            @Override public int applyAsInt(int bound) { assertTrue(index<values.length,"Unexpected random draw"); int value=values[index++]; assertTrue(value>=0 && value<bound); return value; }
        };
    }
    @Test void gearChangesSeverityButCannotUnlockResearchOrReactivateDormantWarp() {
        var state=knowledge(); state.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,20); state.setWarpCounter(100);
        var result=WarpEventRules.roll(state,30,false,rolls(10,32));
        assertTrue(result.triggered()); assertEquals(66,result.strength()); assertEquals(0,result.lastingWarp());
        assertEquals(WarpEventRules.Event.MIST,result.event()); assertEquals(80,state.warpCounter());
        state.setWarpCounter(0); assertFalse(WarpEventRules.roll(state,100,false,rolls(0)).triggered());
    }
    @Test void inclusiveChanceAndNegativeCounterPreserveLegacyQuietPeriod() {
        var state=knowledge(); state.setWarp(PlayerKnowledge.WarpKind.STICKY,26); state.setWarpCounter(1);
        assertFalse(WarpEventRules.roll(state,0,false,rolls(2)).triggered()); assertEquals(1,state.warpCounter());
        var result=WarpEventRules.roll(state,0,false,rolls(1,0));
        assertTrue(result.triggered()); assertEquals(26,result.lastingWarp()); assertEquals(-4,state.warpCounter());
        assertEquals(WarpEventRules.Event.NONE,result.event());
    }
    @Test void cleansedPlayerCannotTriggerFromStaleCounterAndEquipment() {
        var state=knowledge();state.setWarpCounter(100);
        assertFalse(WarpEventRules.roll(state,80,false,rolls(0)).triggered());
        assertEquals(100,state.warpCounter());
        state.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,1);
        assertTrue(WarpEventRules.roll(state,80,false,rolls(0,1)).triggered());
    }
    @Test void maskCanSuppressEffectsAndTheOldEmptyBandsRemainEmpty() {
        var state=knowledge(); state.setWarp(PlayerKnowledge.WarpKind.PERMANENT,100); state.setWarpCounter(100);
        assertEquals(WarpEventRules.Event.NONE,WarpEventRules.roll(state,0,true,rolls(0,4,3)).event());
        for (int value:new int[]{0,5,8,85,88}) assertEquals(WarpEventRules.Event.NONE,WarpEventRules.event(value));
        assertEquals(WarpEventRules.Event.LONG_HUNGER,WarpEventRules.event(73));
        assertEquals(WarpEventRules.Event.RELIEF,WarpEventRules.event(76));
        assertEquals(WarpEventRules.Event.REAL_SPIDERS,WarpEventRules.event(92));
        assertEquals(WarpEventRules.Event.MOST_GUARDIANS,WarpEventRules.event(93));
    }
}
