package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchBranchServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=80)
    public static void branchTablePreferencesAutomationAndCosts(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var state=TCTables.TABLE.get().defaultBlockState().setValue(TableBlock.PART,TableBlock.Part.MASTER).setValue(TableBlock.FACING,Direction.EAST);
        level.setBlock(pos,state,2);level.setBlock(pos.east(),state.setValue(TableBlock.PART,TableBlock.Part.EXTENSION).setValue(TableBlock.FACING,Direction.WEST),2);
        var table=(ResearchTableBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BranchTable"));player.setPos(pos.getCenter());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var data=KnowledgeSavedData.get(player.server);data.ensure(player.getUUID(),bound->0);var menu=new ResearchTableMenu(41,player.getInventory(),table);player.containerMenu=menu;
        test.assertTrue(ResearchCatalogs.catalog().find("ENDLESSRESEARCH").isEmpty(),"Obsolete research removed from catalog");
        test.assertTrue(!ResearchTableAutomation.unlocked(player)&&!menu.clickMenuButton(player,24),"Automation requires its research unlock");
        data.update(player.getUUID(),bound->0,k->{ResearchCatalogs.catalog().all().forEach(e->k.rememberResearch(e.key()));for(String key:List.of("THAUMIUM","NITOR","ALUMENTUM"))k.forgetResearch(key);return true;});
        test.assertTrue(menu.clickMenuButton(player,22)&&menu.clickMenuButton(player,23)&&menu.clickMenuButton(player,24),"Authorized preferences accepted");
        var preferences=data.snapshot(player.getUUID()).orElseThrow().researchPreferences();
        test.assertTrue(preferences.sort()==ResearchPreferences.Sort.QUANTITY&&preferences.descending()&&preferences.autoRefill(),"Preferences saved per player");
        var saved=KnowledgeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()),TCAspects.catalog());test.assertTrue(saved.snapshot(player.getUUID()).orElseThrow().researchPreferences().equals(preferences),"Preferences survive disk format");
        var second=new ResearchTableMenu(42,player.getInventory(),table);test.assertTrue(second.preferences().equals(preferences),"A new menu retrieves same preferences");
        var note=new ResearchNotes(TCAspects.catalog()).generate(ResearchCatalogs.catalog().require("THAUMIUM"),level.random::nextInt).orElseThrow();
        table.items.setStackInSlot(1,ResearchNotesItem.create(new ResearchNote(note.key(),note.color(),true,note.copies(),note.grid())));
        var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());ink.setDamageValue(100);table.items.setStackInSlot(0,ink);
        var pending=ResearchNotesItem.create(new ResearchNotes(TCAspects.catalog()).generate(ResearchCatalogs.catalog().require("NITOR"),level.random::nextInt).orElseThrow());player.getInventory().setItem(0,pending);player.getInventory().setItem(1,new ItemStack(Items.INK_SAC,2));
        test.runAtTickTime(1,()->{
            menu.broadcastChanges();test.assertTrue(table.items.getStackInSlot(1).isEmpty(),"Discovery is consumed immediately");
            test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().researchMarkers().contains("THAUMIUM"),"Discovery grants real knowledge");
        });
        test.runAtTickTime(10,()->{menu.broadcastChanges();test.assertTrue(table.items.getStackInSlot(1).isEmpty(),"No early refill before twenty ticks");});
        test.runAtTickTime(22,()->{
            menu.broadcastChanges();test.assertTrue(table.items.getStackInSlot(1).is(TCItems.RESEARCH_NOTES.get())&&pending.isEmpty(),"Next note moved, not copied");
            test.assertTrue(ScribingToolsItem.ink(table.items.getStackInSlot(0))==100&&player.getInventory().countItem(Items.INK_SAC)==1,"Refill uses existing recipe and one ink ingredient");
            player.getInventory().setItem(2,new ItemStack(Items.PAPER,3));var pen=new ItemStack(TCItems.SCRIBING_TOOLS.get());player.getInventory().setItem(3,pen);
            test.assertTrue(menu.clickMenuButton(player,25),"Random eligible research generated");
            test.assertTrue(player.getInventory().countItem(Items.PAPER)==2&&pen.getDamageValue()==1,"Same paper and ink debit as book");
            test.assertTrue(player.getInventory().items.stream().filter(s->s.is(TCItems.RESEARCH_NOTES.get())).allMatch(s->ResearchNoteNbt.read(s.getTag(),TCAspects.catalog()).note().orElseThrow().key().equals("ALUMENTUM")),"Current table note excluded from random selection");
            boolean before=TCConfig.SERVER.branchFeatures.get();try {
                TCConfig.SERVER.branchFeatures.set(false);test.assertTrue(!menu.clickMenuButton(player,20)&&!menu.clickMenuButton(player,25)&&!ResearchTableAutomation.unlocked(player),"Global switch blocks all table branches");
                test.assertTrue(!menu.clickMenuButton(player,24),"Global switch also blocks automation preferences");
                test.assertTrue(!TCItems.GOGGLES.get().canEquip(new top.theillusivec4.curios.api.SlotContext("head",player,0,false,true),new ItemStack(TCItems.GOGGLES.get())),"Head-slot goggles disabled");
            }finally {TCConfig.SERVER.branchFeatures.set(before);}
            player.setPos(pos.getCenter().add(30,0,0));test.assertTrue(!menu.clickMenuButton(player,24),"Remote menu action rejected");player.containerMenu=player.inventoryMenu;test.succeed();
        });
    }
}
