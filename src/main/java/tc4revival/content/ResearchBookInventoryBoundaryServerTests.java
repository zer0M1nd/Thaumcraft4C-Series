package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchBookInventoryBoundaryServerTests {
    @GameTest(template="wand_test_empty")
    public static void researchNoteInventoryBoundariesPreserveLegacyCosts(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(2,2,2));
        var box=new net.minecraft.world.phys.AABB(pos).inflate(3);
        var knowledge=new PlayerKnowledge(TCAspects.catalog());
        var definition=ResearchCatalogs.catalog().require("THAUMIUM");
        knowledge.rememberResearch("@THAUMIUM");
        for(var key:definition.parents())knowledge.rememberResearch(key);
        for(var key:definition.hiddenParents())knowledge.rememberResearch(key);
        // Full inventory, with and without consuming the last paper, and creative ink exemption.
        for(boolean creative:new boolean[]{false,true}) for(int paperCount:new int[]{1,2}) {
            var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookBoundary"));
            player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            player.setGameMode(creative?net.minecraft.world.level.GameType.CREATIVE:net.minecraft.world.level.GameType.SURVIVAL);
            for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
            var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());ink.setDamageValue(9);
            player.getInventory().setItem(7,ink);player.getInventory().setItem(35,new ItemStack(Items.PAPER,paperCount));
            var before=new HashSet<>(test.getLevel().getEntitiesOfClass(ItemEntity.class,box).stream().map(e->e.getUUID()).toList());
            var result=ResearchBookInventory.requestNote(player,knowledge,"THAUMIUM",0);
            var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!before.contains(e.getUUID()));
            try {
                test.assertTrue(result.status()==ResearchBookInventory.Status.CREATED,"Inventory boundary creates note");
                test.assertTrue(player.getInventory().countItem(Items.PAPER)==paperCount-(creative?0:1) && ink.getDamageValue()==(creative?9:10),"Creative preserves both paper and ink");
                if(paperCount==1&&!creative) test.assertTrue(drops.isEmpty() && player.getInventory().countItem(TCItems.RESEARCH_NOTES.get())==1,"Last paper frees the output slot");
                else test.assertTrue(drops.size()==1 && ItemStack.matches(drops.get(0).getItem(),result.note().orElseThrow()) && player.getInventory().countItem(TCItems.RESEARCH_NOTES.get())==0,"Full inventory drops the exact note in both game modes");
            } finally { drops.forEach(ItemEntity::discard); }
        }
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookSlots"));
        var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());
        player.getInventory().offhand.set(0,ink);player.getInventory().items.set(0,new ItemStack(Items.PAPER,2));
        test.assertTrue(ResearchBookInventory.requestNote(player,knowledge,"THAUMIUM",0).status()==ResearchBookInventory.Status.CREATED && ink.getDamageValue()==1,"Offhand ink is usable");
        player.getInventory().clearContent();
        player.getInventory().offhand.set(0,new ItemStack(Items.PAPER,2));player.getInventory().items.set(0,ink);
        test.assertTrue(ResearchBookInventory.requestNote(player,knowledge,"THAUMIUM",0).status()==ResearchBookInventory.Status.CREATED&&player.getOffhandItem().getCount()==1,"Offhand paper is usable");
        player.getInventory().clearContent();player.getInventory().items.set(0,ink);
        player.getInventory().items.set(35,new ItemStack(Items.PAPER,2));
        test.assertTrue(ResearchBookInventory.requestNote(player,knowledge,"THAUMIUM",0).status()==ResearchBookInventory.Status.CREATED,"Last main inventory slot eligible");
        tc4revival.TC4Revival.LOGGER.info("PASS researchNoteInventoryBoundariesPreserveLegacyCosts: full inventory, last paper, creative, offhand and last main slot");test.succeed();
    }
}
