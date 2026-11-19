package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WandUseTargetTest {
    @Test void targetRoundTripAndCleanupPreserveWandComponentsAndCharge() {
        var tag = new CompoundTag(); tag.putString("rod", "silverwood"); tag.putInt("aer", 350);
        var target = new WandUseTarget(-100, -60, 500, "minecraft:overworld"); target.write(tag);
        assertEquals(target, WandUseTarget.read(tag, "minecraft:overworld").orElseThrow());
        WandUseTarget.clear(tag);
        assertTrue(WandUseTarget.read(tag, "minecraft:overworld").isEmpty());
        assertEquals("silverwood", tag.getString("rod")); assertEquals(350, tag.getInt("aer"));
        assertFalse(tag.contains("IIUX")); assertFalse(tag.contains("IIUY")); assertFalse(tag.contains("IIUZ"));
    }
    @Test void malformedOrCrossDimensionTargetsCannotSelectAnUnrelatedBlockEntity() {
        var tag = new CompoundTag(); new WandUseTarget(1, 2, 3, "minecraft:overworld").write(tag);
        assertTrue(WandUseTarget.read(tag, "minecraft:the_nether").isEmpty());
        tag.remove("tc4_use_dimension"); assertTrue(WandUseTarget.read(tag, "minecraft:overworld").isEmpty());
        new WandUseTarget(1, 2, 3, "minecraft:overworld").write(tag);
        tag.putString("IIUY", "2"); assertTrue(WandUseTarget.read(tag, "minecraft:overworld").isEmpty());
        assertTrue(WandUseTarget.read(null, "minecraft:overworld").isEmpty()); WandUseTarget.clear(null);
    }
}
