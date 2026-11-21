package automagy;

import automagy.content.ItemFilter;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;

import java.util.List;
import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class ItemFilterTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void largeThresholdSurvivesNbtWithoutCreatingRealStacks(net.minecraft.gametest.framework.GameTestHelper h) {
        ItemFilter filter = new ItemFilter(); ItemStack source = new ItemStack(Items.IRON_INGOT, 32);
        filter.entry(0, source); filter.count(0, 999); filter.useCount = true;
        ItemFilter restored = ItemFilter.load(filter.save());
        assertEquals(999, restored.count(0)); assertEquals(1, restored.entry(0).getCount());
        assertEquals(32, source.getCount());
        ItemStack copy = restored.entry(0); copy.shrink(1); assertFalse(restored.entry(0).isEmpty());
        assertFalse(restored.tally(List.of(new ItemStack(Items.IRON_INGOT, 998)), false, true));
        assertTrue(restored.tally(List.of(new ItemStack(Items.IRON_INGOT, 500), new ItemStack(Items.IRON_INGOT, 499)), false, true));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void damageAndOtherNbtAreIndependentOptions(net.minecraft.gametest.framework.GameTestHelper h) {
        ItemFilter filter = new ItemFilter(); ItemStack a = new ItemStack(Items.IRON_PICKAXE), b = a.copy();
        b.setDamageValue(10); assertFalse(filter.matches(a, b));
        filter.ignoreNbt = true; assertFalse(filter.matches(a, b));
        filter.ignoreDamage = true; filter.ignoreNbt = false; assertTrue(filter.matches(a, b));
        b.getOrCreateTag().putString("other", "data"); assertFalse(filter.matches(a, b));
        filter.ignoreNbt = true; assertTrue(filter.matches(a, b));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void savedInvalidThresholdsAndLongNamesAreBounded(net.minecraft.gametest.framework.GameTestHelper h) {
        ItemFilter filter = new ItemFilter(); filter.entry(0, new ItemStack(Items.STONE));
        filter.count(0, Integer.MAX_VALUE); filter.name("x".repeat(500));
        assertEquals(999, filter.count(0)); assertEquals(128, filter.name().length());
        filter.count(0, -10); assertEquals(1, filter.count(0));
        var nbt = filter.save(); nbt.getList("Entries", 10).getCompound(0).putInt("Slot", -1);
        assertTrue(ItemFilter.load(nbt).entry(0).isEmpty());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void logisticsCountsMeanSourceReserveOrDestinationTarget(net.minecraft.gametest.framework.GameTestHelper h) {
        ItemFilter filter = new ItemFilter(); var iron = new ItemStack(Items.IRON_INGOT);
        filter.entry(0, iron); filter.count(0, 16); filter.useCount = true;
        var white = filter.transferLimit(iron, false, 50, 10);
        assertEquals(6, white.maximum()); assertEquals(3, white.overridesMode());
        var black = filter.transferLimit(iron, true, 50, 10);
        assertEquals(34, black.maximum()); assertEquals(2, black.overridesMode());
        assertEquals(0, filter.transferLimit(iron, false, 50, 20).maximum());
        assertEquals(0, filter.transferLimit(new ItemStack(Items.GOLD_INGOT), false, 50, 0).maximum());
        assertEquals(Long.MAX_VALUE, filter.transferLimit(new ItemStack(Items.GOLD_INGOT), true, 50, 0).maximum());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void aggregatedInventariumCountsDoNotOverflowOrLoseNbt(net.minecraft.gametest.framework.GameTestHelper h) {
        ItemFilter filter=new ItemFilter();var iron=new ItemStack(Items.IRON_INGOT);iron.getOrCreateTag().putString("batch","a");
        filter.entry(0,iron);filter.count(0,999);filter.useCount=true;
        var enormous=List.of(new automagy.content.InventariumContents.Entry(iron,Long.MAX_VALUE),new automagy.content.InventariumContents.Entry(iron,100));
        assertTrue(filter.tallySnapshot(enormous,false,true));assertFalse(filter.tallySnapshot(enormous,true,true));
        assertFalse(filter.tallySnapshot(List.of(new automagy.content.InventariumContents.Entry(new ItemStack(Items.IRON_INGOT),Long.MAX_VALUE)),false,true));
        assertEquals(1,iron.getCount());
    h.succeed();
    }
}
