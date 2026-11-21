package advancedthaumaturgy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NodeOperationTest {
    @Test void correctionOperationsRejectWrongNode(){
        assertTrue(NodeOperation.REMOVE_UNSTABLE.available("UNSTABLE","",3));assertFalse(NodeOperation.REMOVE_UNSTABLE.available("NORMAL","",3));
        assertTrue(NodeOperation.REMOVE_HUNGRY.available("HUNGRY","",3));assertFalse(NodeOperation.REMOVE_HUNGRY.available("DARK","",3));
        assertTrue(NodeOperation.REMOVE_FADING.available("NORMAL","FADING",3));assertFalse(NodeOperation.REMOVE_FADING.available("NORMAL","BRIGHT",3));
    }
    @Test void aspectBoundsPreventOverfillingAndEmptyIncrease(){
        assertTrue(NodeOperation.ADD_ASPECT.available("NORMAL","",5));assertFalse(NodeOperation.ADD_ASPECT.available("NORMAL","",6));
        assertFalse(NodeOperation.INCREASE_ASPECT.available("NORMAL","",0));assertTrue(NodeOperation.INCREASE_ASPECT.available("NORMAL","",1));
    }
    @Test void purityAndBrightnessRespectOriginalRestrictions(){
        assertTrue(NodeOperation.ADD_BRIGHT.available("PURE","",2));assertFalse(NodeOperation.ADD_BRIGHT.available("NORMAL","PALE",2));
        assertTrue(NodeOperation.ADD_PURE.available("DARK","",2));assertFalse(NodeOperation.ADD_PURE.available("PURE","",2));assertFalse(NodeOperation.ADD_PURE.available("NORMAL","BRIGHT",2));
    }
    @Test void networkButtonBoundsAndCostBaseline(){
        assertTrue(NodeOperation.byId(-1).isEmpty());assertTrue(NodeOperation.byId(13).isEmpty());assertTrue(NodeOperation.byId(Integer.MAX_VALUE).isEmpty());
        assertEquals(32,NodeOperation.ADD_ASPECT.amount);assertEquals(4,NodeOperation.ADD_ASPECT.essences);
        assertEquals("fames",NodeOperation.ADD_HUNGRY.essentia);assertEquals(64,NodeOperation.ADD_HUNGRY.amount);assertEquals(2,NodeOperation.ADD_HUNGRY.essences);
    }
}
