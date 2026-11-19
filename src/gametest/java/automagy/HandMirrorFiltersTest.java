package automagy;

import automagy.content.*;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;

import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class HandMirrorFiltersTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy")
    public static void mirrorSlotsRequirePopulatedFilters(net.minecraft.gametest.framework.GameTestHelper h) {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MirrorFilterTest"));
        var mirror=new ItemStack(Content.AUTO_HAND_MIRROR.get());
        tc4revival.content.ItemMirrorItem.writeDestination(mirror.getOrCreateTag(),net.minecraft.core.GlobalPos.of(h.getLevel().dimension(),h.absolutePos(net.minecraft.core.BlockPos.ZERO)),"test");
        player.getInventory().setItem(0,mirror);
        try {
            var menu=new AutoHandMirrorMenu(1,player.getInventory(),0);
            for(var item:List.of(Content.WHITE_FILTER.get(),Content.BLACK_FILTER.get())) {
                var paper=new ItemStack(item);
                for(int slot=1;slot<=2;slot++)h.assertTrue(!menu.getSlot(slot).mayPlace(paper),"Empty filter was accepted");
                var filter=new ItemFilter();filter.useCount=true;
                paper.getOrCreateTag().put("Filter",filter.save());
                h.assertTrue(!menu.getSlot(1).mayPlace(paper),"Options alone must not populate a filter");
                filter.name("iron");paper.getOrCreateTag().put("Filter",filter.save());
                h.assertTrue(menu.getSlot(1).mayPlace(paper),"Name-only filter rejected");
                filter.name("");filter.entry(0,new ItemStack(Items.IRON_INGOT));paper.getOrCreateTag().put("Filter",filter.save());
                h.assertTrue(menu.getSlot(2).mayPlace(paper),"Item filter rejected");
            }
            h.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(Items.PAPER)),"Ordinary paper accepted");
            menu.removed(player);
        } finally { player.getInventory().clearContent(); }
        h.succeed();
    }
    private static HandMirrorFilters.Rule rule(boolean black,boolean count,int amount){
        var filter=new ItemFilter();filter.entry(0,new ItemStack(Items.IRON_INGOT));filter.useCount=count;filter.count(0,amount);return new HandMirrorFilters.Rule(filter,black);
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void emptyAndUnmatchedFiltersDoNotVacuumTheInventory(net.minecraft.gametest.framework.GameTestHelper h) {
        var iron=new ItemStack(Items.IRON_INGOT);assertEquals(0,HandMirrorFilters.maximum(List.of(),iron,List.of(),64));
        assertEquals(0,HandMirrorFilters.maximum(List.of(rule(true,false,1)),new ItemStack(Items.GOLD_INGOT),List.of(),64));
        assertEquals(0,HandMirrorFilters.maximum(List.of(rule(true,false,1)),iron,List.of(),64));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void whiteCountLimitsEachPickupAndBlackCountRetainsPlayerReserve(net.minecraft.gametest.framework.GameTestHelper h) {
        var iron=new ItemStack(Items.IRON_INGOT);var held=List.of(new ItemStack(Items.IRON_INGOT,12));
        assertEquals(5,HandMirrorFilters.maximum(List.of(rule(false,true,5)),iron,held,20));
        assertEquals(16,HandMirrorFilters.maximum(List.of(rule(true,true,16)),iron,held,20));
        assertEquals(0,HandMirrorFilters.maximum(List.of(rule(true,true,40)),iron,held,20));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void explicitEntriesOutrankEarlierNameRulesAndFirstExplicitEntryWins(net.minecraft.gametest.framework.GameTestHelper h) {
        var named=new ItemFilter();named.name("*");var iron=new ItemStack(Items.IRON_INGOT);
        assertEquals(7,HandMirrorFilters.maximum(List.of(new HandMirrorFilters.Rule(named,true),rule(false,true,7)),iron,List.of(),20));
        assertEquals(0,HandMirrorFilters.maximum(List.of(rule(true,false,1),rule(false,true,7)),iron,List.of(),20));
        assertEquals(20,HandMirrorFilters.maximum(List.of(new HandMirrorFilters.Rule(named,false)),iron,List.of(),20));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void reserveAggregatesOnlyMatchingNbtUnlessIgnored(net.minecraft.gametest.framework.GameTestHelper h) {
        var r=rule(true,true,16);var distinct=new ItemStack(Items.IRON_INGOT,64);distinct.getOrCreateTag().putString("batch","other");var iron=new ItemStack(Items.IRON_INGOT);
        assertEquals(4,HandMirrorFilters.maximum(List.of(r),iron,List.of(distinct),20));
        r.filter().ignoreNbt=true;assertEquals(20,HandMirrorFilters.maximum(List.of(r),iron,List.of(distinct),20));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void secondMirrorSeesOnlyUnsentIncomingItems(net.minecraft.gametest.framework.GameTestHelper h) {
        var iron=new ItemStack(Items.IRON_INGOT);var held=List.of(new ItemStack(Items.IRON_INGOT,12));
        int incoming=20;
        int first=HandMirrorFilters.maximum(List.of(rule(false,true,7)),iron,held,incoming);
        int second=HandMirrorFilters.maximum(List.of(rule(true,true,16)),iron,held,incoming-first);
        assertEquals(7,first);assertEquals(9,second);assertEquals(4,incoming-first-second);
    h.succeed();
    }
}
