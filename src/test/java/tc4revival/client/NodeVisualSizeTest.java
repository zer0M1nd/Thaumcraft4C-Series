package tc4revival.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NodeVisualSizeTest {
    @Test void ordinaryNodesRetainTheirSizeAndCommandNodesStopGrowingAtOneHundred() {
        for(int amount:new int[]{0,1,50,99,100})assertEquals(amount,NodeVisualSize.amount(amount));
        for(int amount:new int[]{101,10000,Integer.MAX_VALUE})assertEquals(NodeVisualSize.amount(100),NodeVisualSize.amount(amount));
        assertEquals(0,NodeVisualSize.amount(Integer.MIN_VALUE));
    }
}
