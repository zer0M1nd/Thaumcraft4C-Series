package tc4revival.content;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class WandPartsTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void oldTagsPreserveStaffAndSceptreIndependence() {
        var nbt = new CompoundTag(); nbt.putString("rod", "primal_staff"); nbt.putByte("sceptre", (byte) 0);
        assertTrue(WandParts.rod(nbt).staff()); assertTrue(WandParts.rod(nbt).runes());
        assertTrue(WandParts.sceptre(nbt)); assertEquals(37500, WandParts.capacity(nbt));
        nbt.remove("sceptre"); assertEquals(25000, WandParts.capacity(nbt));
        nbt.putString("rod", "invalid"); assertEquals(WandParts.Rod.WOOD, WandParts.rod(nbt));
        assertEquals(WandParts.Cap.IRON, WandParts.cap(nbt));
    }
    @Test void copperOnlyChangesOrderAndEntropy() {
        assertEquals(1f, WandParts.Cap.COPPER.cost("ordo")); assertEquals(1f, WandParts.Cap.COPPER.cost("perditio"));
        assertEquals(1.1f, WandParts.Cap.COPPER.cost("aer"));
        assertEquals("thaumcraft:textures/models/wand_rod_blaze.png", WandParts.Rod.BLAZE_STAFF.texture());
    }
    @Test void silverUsesLegacyFourElementDiscountAndPreservesNbtIdentity() {
        for(String aspect:java.util.List.of("aer","terra","ignis","aqua"))
            assertEquals(.95f,WandParts.Cap.SILVER.cost(aspect));
        for(String aspect:java.util.List.of("ordo","perditio"))
            assertEquals(1f,WandParts.Cap.SILVER.cost(aspect));
        assertEquals(4,WandParts.Cap.SILVER.craftCost);
        var tag=new CompoundTag();tag.putString("cap","silver");
        assertSame(WandParts.Cap.SILVER,WandParts.cap(tag));
    }
    @Test void elementalRegenerationUsesPlayerTicksAndStopsAtTenthCapacity() {
        var nbt = new CompoundTag(); nbt.putString("rod", "reed"); nbt.putInt("aer", 749);
        assertTrue(WandRegeneration.tick(nbt, 199, aspects, bound -> { fail("No RNG for elemental rods"); return 0; }).isEmpty());
        assertEquals("aer", WandRegeneration.tick(nbt, 200, aspects, bound -> 0).orElseThrow()); assertEquals(750, nbt.getInt("aer"));
        assertTrue(WandRegeneration.tick(nbt, 400, aspects, bound -> 0).isEmpty());
    }
    @Test void primalStaffSelectsOnlyAnUnderfilledPrimal() {
        var nbt = new CompoundTag(); nbt.putString("rod", "primal_staff");
        aspects.all().stream().filter(a -> a.isPrimal()).forEach(a -> nbt.putInt(a.tag(), 2500));
        assertTrue(WandRegeneration.tick(nbt, 50, aspects, bound -> { fail("No eligible aspect"); return 0; }).isEmpty());
        nbt.putInt("ignis", 2499);
        assertEquals("ignis", WandRegeneration.tick(nbt, 100, aspects, bound -> { assertEquals(1, bound); return 0; }).orElseThrow());
        assertEquals(2500, nbt.getInt("ignis"));
    }
}
