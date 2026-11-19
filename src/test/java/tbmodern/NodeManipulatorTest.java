package tbmodern;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tc4revival.content.AuraNodeModifier;
import tc4revival.content.AuraNodeType;

class NodeManipulatorTest {
    @Test void brightnessRequiresNormalQualityAndTwentyMinutes() {
        var change = NodeManipulator.transformation("brightness",AuraNodeType.PURE,null);
        assertEquals(24000,change.ticks());assertEquals(AuraNodeType.PURE,change.type());assertEquals(AuraNodeModifier.BRIGHT,change.modifier());
        assertNull(NodeManipulator.transformation("brightness",AuraNodeType.NORMAL,AuraNodeModifier.PALE));
    }
    @Test void stabilityRepairsQualityBeforeChangingType() {
        var pale = NodeManipulator.transformation("stability",AuraNodeType.HUNGRY,AuraNodeModifier.PALE);
        assertEquals(AuraNodeType.HUNGRY,pale.type());assertNull(pale.modifier());assertEquals(12000,pale.ticks());
        var hungry = NodeManipulator.transformation("stability",AuraNodeType.HUNGRY,null);
        assertEquals(AuraNodeType.NORMAL,hungry.type());assertEquals(600,hungry.ticks());
    }
    @Test void purityCleansTaintBeforeProducingPureNode() {
        var change = NodeManipulator.transformation("purity",AuraNodeType.TAINTED,AuraNodeModifier.BRIGHT);
        assertEquals(AuraNodeType.NORMAL,change.type());assertEquals(AuraNodeModifier.BRIGHT,change.modifier());assertEquals(39600,change.ticks());
    }
    @Test void destructionRetainsLegacyFinalDeviceRemoval() {
        assertTrue(NodeManipulator.transformation("destruction",AuraNodeType.NORMAL,AuraNodeModifier.FADING).destroyNode());
        assertNull(NodeManipulator.transformation("speed",AuraNodeType.NORMAL,null));
    }
}
