package tc4revival.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchColumnSelectionTest {
    @Test void sevenColumnsKeepSelectionCenteredWherePossible() {
        var state=new ResearchColumnSelection(0);
        int[] expected={0,0,0,1,2,2,2};
        for(int i=0;i<7;i++){assertEquals(i,state.selected());assertEquals(expected[i],state.windowStart(33));state.move(1,33);}
        assertEquals(6,state.selected());assertEquals(30,state.index(0,33));assertEquals(32,state.index(2,33));assertEquals(-1,state.index(3,33));
        state.move(-100,33);assertEquals(0,state.selected());
    }
    @Test void searchEditsResetFilteredSelectionAndClearingRestoresUnfilteredPosition() {
        var state=new ResearchColumnSelection(5);
        state.queryChanged(true,20);assertEquals(0,state.selected());state.move(2,20);
        state.queryChanged(true,40);assertEquals(0,state.selected());assertEquals(5,state.remembered());
        state.queryChanged(true,0);state.queryChanged(false,33);assertEquals(5,state.selected());assertEquals(2,state.windowStart(33));
    }
    @Test void emptyAndSmallListsHaveNoPhantomColumnsOrRows() {
        var state=new ResearchColumnSelection(0);
        assertEquals(0,state.windowStart(0));assertEquals(-1,state.index(0,0));
        assertEquals(0,ResearchColumnSelection.lastColumn(5));assertEquals(1,ResearchColumnSelection.lastColumn(6));
        state.move(1,6);assertEquals(5,state.index(0,6));assertEquals(-1,state.index(1,6));assertEquals(0,state.windowStart(6));
    }
    @Test void stickySelectionTogglesSwitchesAndYieldsToDragging() {
        var state=new ResearchColumnSelection(0);
        state.pick("aer");assertEquals("aer",state.picked());state.pick("aer");assertNull(state.picked());
        state.pick("aer");state.pick("terra");assertEquals("terra",state.picked());state.cancelPick();assertNull(state.picked());
        state.pick(null);assertNull(state.picked());
    }
    @Test void MissingKnowledgeDuringOpenOrSearchClearDoesNotEraseSavedColumn() {
        var state=new ResearchColumnSelection(5);state.clamp(0);state.move(1,0);state.clamp(33);
        assertEquals(5,state.selected());state.queryChanged(true,10);state.queryChanged(false,0);state.clamp(33);
        assertEquals(5,state.selected());assertEquals(5,state.remembered());
    }
}
