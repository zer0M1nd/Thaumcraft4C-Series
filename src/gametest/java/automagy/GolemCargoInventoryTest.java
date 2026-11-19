package automagy;

import automagy.content.*;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraftforge.items.ItemStackHandler;

import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class GolemCargoInventoryTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void extractionFromSourcePersistsOnlyWhatTheGolemCanActuallyCarry(net.minecraft.gametest.framework.GameTestHelper h) {
        var held=new AtomicReference<>(ItemStack.EMPTY);var cargo=new GolemCargoInventory(held::get,held::set,()->8);
        var source=new ItemStackHandler(1);source.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,20));
        assertEquals(8,InventoryTransfer.move(source,cargo,new ItemStack(Items.IRON_INGOT),20,ItemStack::isSameItemSameTags,item->fail("Unexpected spill")));
        assertEquals(12,source.getStackInSlot(0).getCount());assertEquals(8,held.get().getCount());
        var full=new ItemStackHandler(1);full.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));
        assertEquals(0,InventoryTransfer.move(cargo,full,held.get(),8,ItemStack::isSameItemSameTags,item->fail("Must not lose cargo")));
        assertEquals(8,held.get().getCount());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void simulationAndSnapshotsCannotMutateThePersistedCargo(net.minecraft.gametest.framework.GameTestHelper h) {
        var held=new AtomicReference<>(new ItemStack(Items.DIAMOND,3));var cargo=new GolemCargoInventory(held::get,held::set,()->8);
        cargo.getStackInSlot(0).setCount(1);cargo.extractItem(0,2,true).setCount(1);
        assertTrue(cargo.insertItem(0,new ItemStack(Items.DIAMOND,5),true).isEmpty());assertEquals(3,held.get().getCount());
        var tagged=new ItemStack(Items.DIAMOND);tagged.getOrCreateTag().putInt("variant",1);
        assertEquals(1,cargo.insertItem(0,tagged,false).getCount());assertEquals(3,held.get().getCount());
        cargo.extractItem(0,3,false);assertTrue(held.get().isEmpty());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void readingAllowancePreventsExtractionFromUnrecordedSlots(net.minecraft.gametest.framework.GameTestHelper h) {
        var source=new ItemStackHandler(3);source.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,2));source.setStackInSlot(2,new ItemStack(Items.IRON_INGOT,60));
        var held=new AtomicReference<>(ItemStack.EMPTY);var cargo=new GolemCargoInventory(held::get,held::set,()->64);
        var prefix=new InventoryPrefix(source,2);
        assertEquals(2,InventoryTransfer.move(prefix,cargo,new ItemStack(Items.IRON_INGOT),64,ItemStack::isSameItemSameTags,stack->fail("Unexpected spill")));
        assertEquals(60,source.getStackInSlot(2).getCount());assertThrows(IndexOutOfBoundsException.class,()->prefix.extractItem(2,1,false));
        assertEquals(0,new InventoryPrefix(source,-10).getSlots());
    h.succeed();
    }
}
