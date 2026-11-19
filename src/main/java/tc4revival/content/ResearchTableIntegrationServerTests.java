package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.network.ResearchTableActionPacket;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchTableIntegrationServerTests {
    @GameTest(template="wand_test_empty")
    public static void researchTableEditsAndCopiesPreserveInventoryAcrossReload(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var state=TCTables.TABLE.get().defaultBlockState().setValue(TableBlock.PART,TableBlock.Part.MASTER).setValue(TableBlock.FACING,Direction.EAST);
        level.setBlock(pos,state,2);level.setBlock(pos.east(),state.setValue(TableBlock.PART,TableBlock.Part.EXTENSION).setValue(TableBlock.FACING,Direction.WEST),2);
        var table=(ResearchTableBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ResearchEdit"));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var data=KnowledgeSavedData.get(player.server);
        data.update(player.getUUID(),bound->0,k->{for(var a:TCAspects.catalog().all())k.setPool(a,(short)100);return null;});
        var menu=new ResearchTableMenu(9,player.getInventory(),table);player.containerMenu=menu;
        var grid=new java.util.LinkedHashMap<ResearchHex,ResearchNote.Cell>();
        grid.put(new ResearchHex(-1,0),new ResearchNote.Cell(1,"terra"));grid.put(new ResearchHex(1,0),new ResearchNote.Cell(1,"ordo"));
        grid.put(new ResearchHex(0,0),ResearchNote.Cell.blank());grid.put(new ResearchHex(0,1),ResearchNote.Cell.blank());
        var note=ResearchNotesItem.create(new ResearchNote("THAUMIUM",123,false,0,grid));note.getOrCreateTag().putString("audit_custom","retained");
        var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());ink.setDamageValue(ink.getMaxDamage()-3);
        table.items.setStackInSlot(0,ink);table.items.setStackInSlot(1,note);
        new ResearchTableActionPacket(9,ResearchTableBlockEntity.Action.PLACE,0,1,"aer","").handle(player);
        test.assertTrue(ScribingToolsItem.ink(table.items.getStackInSlot(0))==2 && data.snapshot(player.getUUID()).orElseThrow().pool(TCAspects.catalog().require("aer"))==99,"Placement spends one ink and point");
        new ResearchTableActionPacket(9,ResearchTableBlockEntity.Action.ERASE,0,1,"","").handle(player);
        test.assertTrue(ScribingToolsItem.ink(table.items.getStackInSlot(0))==1 && data.snapshot(player.getUUID()).orElseThrow().pool(TCAspects.catalog().require("aer"))==99,"Erase spends ink with no unresearched refund");
        var saved=table.saveWithoutMetadata();table.items.setStackInSlot(0,ItemStack.EMPTY);table.items.setStackInSlot(1,ItemStack.EMPTY);table.load(saved);
        test.assertTrue(table.saveWithoutMetadata().equals(saved),"Partially edited note and ink restore exactly");
        new ResearchTableActionPacket(9,ResearchTableBlockEntity.Action.PLACE,0,0,"vitreus","").handle(player);
        var completed=ResearchNoteNbt.read(table.items.getStackInSlot(1).getTag(),TCAspects.catalog()).note().orElseThrow();
        test.assertTrue(completed.complete() && table.items.getStackInSlot(1).is(TCItems.DISCOVERY.get()) && ScribingToolsItem.ink(table.items.getStackInSlot(0))==0,"Last ink solves and converts note to discovery");
        test.assertTrue(table.items.getStackInSlot(1).getTag().getString("audit_custom").equals("retained"),"Completion preserves unrelated NBT");
        player.getInventory().setItem(0,new ItemStack(Items.PAPER,2));player.getInventory().setItem(1,new ItemStack(Items.INK_SAC,2));
        var duplicate=new ResearchTableActionPacket(9,ResearchTableBlockEntity.Action.DUPLICATE,0,0,"","");
        long before=data.revision(player.getUUID());duplicate.handle(player);
        test.assertTrue(data.revision(player.getUUID())==before && player.getInventory().countItem(Items.PAPER)==2,"Copy requires RESEARCHDUPE");
        data.update(player.getUUID(),bound->0,k->{k.rememberResearch("RESEARCHDUPE");return null;});
        var costs=ResearchCatalogs.catalog().require("THAUMIUM").aspects();
        for(int copy=0;copy<2;copy++) {
            var old=data.snapshot(player.getUUID()).orElseThrow();duplicate.handle(player);
            var current=ResearchNoteNbt.read(table.items.getStackInSlot(1).getTag(),TCAspects.catalog()).note().orElseThrow();
            test.assertTrue(current.copies()==copy+1 && table.items.getStackInSlot(1).getCount()==copy+2 && player.getInventory().countItem(Items.PAPER)==1-copy && player.getInventory().countItem(Items.INK_SAC)==1-copy,"Each copy enlarges shared stack and pays paper/ink sac");
            for(var cost:costs.entrySet()) {var aspect=TCAspects.catalog().require(cost.getKey());test.assertTrue(old.pool(aspect)-data.snapshot(player.getUUID()).orElseThrow().pool(aspect)==cost.getValue()+copy,"Copies add current copy count to every aspect fee");}
            saved=table.saveWithoutMetadata();table.load(saved);test.assertTrue(table.saveWithoutMetadata().equals(saved),"Copied discovery stack persists");
        }
        before=data.revision(player.getUUID());saved=table.saveWithoutMetadata();duplicate.handle(player);
        test.assertTrue(data.revision(player.getUUID())==before && saved.equals(table.saveWithoutMetadata()),"Missing copy materials leave knowledge and inventory untouched");
        player.containerMenu=player.inventoryMenu;
        tc4revival.TC4Revival.LOGGER.info("PASS researchTableEditsAndCopiesPreserveInventoryAcrossReload: place, erase, solve, two copies, costs and note reload");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void researchTablePacketBindingAndCombinationCommitActualKnowledge(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2));
        var master=TCTables.TABLE.get().defaultBlockState().setValue(TableBlock.PART,TableBlock.Part.MASTER).setValue(TableBlock.FACING,Direction.EAST);
        level.setBlock(pos,master,2);
        level.setBlock(pos.east(),master.setValue(TableBlock.PART,TableBlock.Part.EXTENSION).setValue(TableBlock.FACING,Direction.WEST),2);
        var table=(ResearchTableBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ResearchTableTest"));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var data=KnowledgeSavedData.get(player.server);
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis"); var lux=TCAspects.catalog().require("lux");
        data.update(player.getUUID(),bound->0,k->{k.setPool(air,(short)5);k.setPool(fire,(short)5);k.setPool(lux,(short)0);return null;});
        var menu=new ResearchTableMenu(7,player.getInventory(),table); player.containerMenu=menu;
        var combine=new ResearchTableActionPacket(7,ResearchTableBlockEntity.Action.COMBINE,0,0,"aer","ignis");
        long revision=data.revision(player.getUUID());
        new ResearchTableActionPacket(8,ResearchTableBlockEntity.Action.COMBINE,0,0,"aer","ignis").handle(player);
        test.assertTrue(data.revision(player.getUUID())==revision,"Wrong container ID does not commit");
        player.setPos(pos.getX()+20,pos.getY(),pos.getZ()); combine.handle(player);
        test.assertTrue(data.revision(player.getUUID())==revision,"Out of range action does not commit");
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); player.containerMenu=player.inventoryMenu; combine.handle(player);
        test.assertTrue(data.revision(player.getUUID())==revision,"Closed menu does not commit");
        player.containerMenu=menu; combine.handle(player);
        var knowledge=data.snapshot(player.getUUID()).orElseThrow();
        test.assertTrue(knowledge.pool(air)==4 && knowledge.pool(fire)==4 && knowledge.pool(lux)==1 && data.revision(player.getUUID())==revision+1,"Actual handler commits component costs and known compound reward once");
        test.assertTrue(table.items.getStackInSlot(0).isEmpty() && table.items.getStackInSlot(1).isEmpty(),"Combination needs neither ink nor note");
        // Same operands require two available points before either is debited.
        new ResearchTableActionPacket(7,ResearchTableBlockEntity.Action.COMBINE,0,0,"aer","aer").handle(player);
        test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().pool(air)==2,"Invalid identical combination consumes both points when available");
        data.update(player.getUUID(),bound->0,k->{k.setPool(air,(short)1);return null;});
        new ResearchTableActionPacket(7,ResearchTableBlockEntity.Action.COMBINE,0,0,"aer","aer").handle(player);
        test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().pool(air)==1,"Insufficient identical operands leave the remaining point untouched");
        var saved=table.saveWithoutMetadata(); var loaded=new ResearchTableBlockEntity(pos,master); loaded.load(saved);
        test.assertTrue(loaded.saveWithoutMetadata().equals(saved),"Actual table inventory/bonus payload roundtrips");
        data.update(player.getUUID(),bound->0,k->{k.setPool(air,(short)5);return null;});
        var before=data.revision(player.getUUID()); level.removeBlockEntity(pos); combine.handle(player);
        test.assertTrue(data.revision(player.getUUID())==before,"Stale BE identity cannot act after removal");
        player.containerMenu=player.inventoryMenu;
        tc4revival.TC4Revival.LOGGER.info("PASS researchTablePacketBindingAndCombinationCommitActualKnowledge: menu ID, range, closed/stale binding, actual combination and save/load");
        test.succeed();
    }
}
