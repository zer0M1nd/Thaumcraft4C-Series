package tc4revival.content;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WandAssemblyTest {
    private ArrayList<WandAssembly.Ingredient> grid(boolean sceptre, int cap, int rod) {
        var grid = new ArrayList<WandAssembly.Ingredient>(Collections.nCopies(9, null));
        var c = new WandAssembly.Ingredient("Thaumcraft:WandCap", cap, null);
        grid.set(4, rod == -1 ? new WandAssembly.Ingredient("minecraft:stick", 0, null)
                : new WandAssembly.Ingredient("Thaumcraft:WandRod", rod, null));
        if (sceptre) { grid.set(1, c); grid.set(5, c); grid.set(6, c); grid.set(2, new WandAssembly.Ingredient("Thaumcraft:ItemResource", 15, null)); }
        else { grid.set(2, c); grid.set(6, c); }
        return grid;
    }
    @Test void normalAndStaffUseTwoCapsAndEqualPrimalCosts() {
        var ordinary = WandAssembly.match(grid(false, 1, 0), false, key -> true).orElseThrow();
        assertEquals(9, ordinary.costPerPrimal()); assertTrue(ordinary.unlocked()); assertTrue(ordinary.hasOutput());
        var staff = WandAssembly.match(grid(false, 2, 52), false, key -> true).orElseThrow();
        assertEquals(144, staff.costPerPrimal()); assertTrue(staff.rod().staff());
        assertFalse(WandAssembly.match(grid(false, 0, -1), false, key -> true).orElseThrow().hasOutput());
    }
    @Test void sceptreRoundsItsCostAndRequiresSeparateResearch() {
        var result = WandAssembly.match(grid(true, 1, 0), true, key -> !key.equals("SCEPTRE")).orElseThrow();
        assertEquals(13, result.costPerPrimal()); assertFalse(result.unlocked()); assertTrue(result.hasOutput());
        assertEquals(1, result.components().getByte("sceptre"));
        result.components().putString("cap", "iron"); assertEquals("gold", result.components().getString("cap"));
    }
    @Test void extraneousCellsAndWrongCapsRejectWithoutChangingGrid() {
        var grid = grid(false, 1, 0); grid.set(0, grid.get(2));
        assertTrue(WandAssembly.match(grid, false, key -> true).isEmpty());
        grid.set(0, null); grid.set(2, new WandAssembly.Ingredient("Thaumcraft:WandCap", 2, null));
        assertTrue(WandAssembly.match(grid, false, key -> true).isEmpty());
        assertEquals(2, grid.get(2).metadata());
    }
    @Test void sceptreIgnoresTagsWhereOrdinaryAssemblyRejectsTaggedComponents() {
        var tag = new CompoundTag(); tag.putInt("custom", 1);
        for (boolean sceptre : new boolean[]{false, true}) {
            var grid = grid(sceptre, 1, 0);
            for (int i = 0; i < 9; i++) if (grid.get(i) != null) {
                var value = grid.get(i); grid.set(i, new WandAssembly.Ingredient(value.item(), value.metadata(), tag));
            }
            assertEquals(sceptre, WandAssembly.match(grid, sceptre, key -> true).isPresent());
        }
    }
}
