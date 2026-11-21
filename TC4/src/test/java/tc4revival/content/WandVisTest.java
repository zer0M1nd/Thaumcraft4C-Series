package tc4revival.content;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;

class WandVisTest {
    private final Aspect air = new Aspect("aer", 0xffffff, java.util.List.of(), "", 1);
    private final Aspect fire = new Aspect("ignis", 0xff0000, java.util.List.of(), "", 1);
    @Test void capacityAndWholeVisOverflowPreserveHundredthsRounding() {
        assertEquals(2500, WandVis.capacity(25, false)); assertEquals(3750, WandVis.capacity(25, true));
        var tag = new CompoundTag(); tag.putInt("aer", 2499);
        assertEquals(0, WandVis.add(tag, air, 1, 2500, true, false)); assertEquals(2499, WandVis.stored(tag, air));
        assertEquals(0, WandVis.add(tag, air, 1, 2500, true, true)); assertEquals(2500, WandVis.stored(tag, air));
        assertEquals(99, WandVis.add(tag, air, 99, 2500, false, true));
    }
    @Test void insufficientLaterAspectAndSimulationNeverDebitEarlierComponents() {
        var tag = new CompoundTag(); tag.putInt("aer", 200); tag.putInt("ignis", 49); tag.putString("rod", "wood");
        var costs = new LinkedHashMap<Aspect, Integer>(); costs.put(air, 100); costs.put(fire, 100);
        var before = tag.copy(); assertFalse(WandVis.consume(tag, costs, a -> .5, false, true)); assertEquals(before, tag);
        tag.putInt("ignis", 50); before = tag.copy();
        assertTrue(WandVis.consume(tag, costs, a -> .5, false, false)); assertEquals(before, tag);
        assertTrue(WandVis.consume(tag, costs, a -> .5, false, true));
        assertEquals(150, tag.getInt("aer")); assertEquals(0, tag.getInt("ignis")); assertEquals("wood", tag.getString("rod"));
    }
    @Test void craftingUnitsAndFractionalCostsAreDistinct() {
        var tag = new CompoundTag(); tag.putInt("aer", 1000);
        assertTrue(WandVis.consume(tag, Map.of(air, 1), a -> .9, false, true)); assertEquals(1000, tag.getInt("aer"));
        assertTrue(WandVis.consume(tag, Map.of(air, 1), a -> .9, true, true)); assertEquals(910, tag.getInt("aer"));
        assertEquals(.7f, WandVis.modifier(1, .1f, 1, true, false, true), .00001f);
        assertEquals(.8f, WandVis.modifier(1, .1f, 1, true, true, true), .00001f);
        assertEquals(.1f, WandVis.modifier(1, 2, 5, true, false, true));
    }
    @Test void invalidCostsCannotCreateChargeOrPartiallyChangeNbt() {
        var tag = new CompoundTag(); tag.putInt("aer", 100); var before = tag.copy();
        assertThrows(IllegalArgumentException.class, () -> WandVis.consume(tag, Map.of(air, -1), a -> 1, false, true));
        assertThrows(ArithmeticException.class, () -> WandVis.consume(tag, Map.of(air, Integer.MAX_VALUE), a -> 1, true, true));
        assertThrows(IllegalArgumentException.class, () -> WandVis.consume(tag, Map.of(air, 1), a -> Double.NaN, false, true));
        assertEquals(before, tag); assertFalse(WandVis.consume(tag, Map.of(), a -> 1, false, true));
    }
}
