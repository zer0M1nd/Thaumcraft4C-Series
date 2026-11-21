package automagy.content;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraftforge.items.ItemStackHandler;

import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class InventariumContentsTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void duplicateMultiblockViewsDoNotConsumeBudgetTwice(net.minecraft.gametest.framework.GameTestHelper h) {
        var shared=new ItemStackHandler(2);shared.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,23));
        var next=new ItemStackHandler(1);next.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,9));
        var first=new InventariumContents.Source(BlockPos.ZERO,null);var second=new InventariumContents.Source(BlockPos.ZERO.east(),null);var third=new InventariumContents.Source(BlockPos.ZERO.east(2),null);
        var result=InventariumContents.readResolved(List.of(first,second,third),3,s->Optional.of(new InventariumContents.Resolved(s.equals(third)?next:shared,s.equals(third)?"next":"shared")));
        assertEquals(3,result.slotsRead());assertFalse(result.omitted());assertEquals(2,result.entries().size());
        assertEquals(23,result.entries().get(0).amount());assertEquals(9,result.entries().get(1).amount());
        result.entries().get(0).item().setCount(0);assertEquals(23,shared.getStackInSlot(0).getCount());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void emptySlotsUseCapacityAndMissingViewsDoNot(net.minecraft.gametest.framework.GameTestHelper h) {
        var source=new InventariumContents.Source(BlockPos.ZERO,null);var missing=new InventariumContents.Source(BlockPos.ZERO.east(),null);
        var inventory=new ItemStackHandler(2);inventory.setStackInSlot(1,new ItemStack(Items.DIAMOND,6));
        var result=InventariumContents.readResolved(List.of(missing,source),1,s->s.equals(missing)?Optional.empty():Optional.of(new InventariumContents.Resolved(inventory,"one")));
        assertEquals(1,result.slotsRead());assertTrue(result.omitted());assertTrue(result.entries().isEmpty());
    h.succeed();
    }
}
