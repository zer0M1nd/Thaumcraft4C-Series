package tc4revival.content;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchBookTransactionServerTests {
    @GameTest(template="wand_test_empty")
    public static void openBookUsesLiveDifficultyForSecondaryResearch(GameTestHelper test) {
        var definition=ResearchCatalogs.catalog().all().stream().filter(e->ResearchCatalogs.enabled(e) && e.has(ResearchDefinition.Flag.SECONDARY) && !e.has(ResearchDefinition.Flag.VIRTUAL) && !e.aspects().isEmpty()).findFirst().orElseThrow();
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookLiveConfig"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.THAUMONOMICON.get()));
        var data=KnowledgeSavedData.get(player.server);int[] difficulty={0};
        var book=new ServerResearchBook(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false,data,()->new ServerResearchBook.Settings(difficulty[0],false,false));
        data.update(player.getUUID(),bound->0,k->{k.wipe();k.rememberResearch("@"+definition.key());k.rememberResearch("ELDRITCHMINOR");for(String p:definition.parents())k.rememberResearch(p);for(String p:definition.hiddenParents())k.rememberResearch(p);for(var a:TCAspects.catalog().all())k.setPool(a,(short)0);return null;});
        var initial=book.request(player,book.token(),0,definition.key());
        test.assertTrue(initial instanceof ServerResearchBook.Purchase p && p.result().rejection().orElseThrow()==ResearchBookActions.Rejection.POINTS,"Normal secondary route buys with points");
        var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());player.getInventory().setItem(1,ink);player.getInventory().setItem(2,new ItemStack(Items.PAPER,2));
        difficulty[0]=1;
        var note=book.request(player,book.token(),1,definition.key());
        test.assertTrue(note instanceof ServerResearchBook.Note n && n.result().status()==ResearchBookInventory.Status.CREATED,"Same open session changes to a physical note when difficulty becomes hard");
        test.assertTrue(ink.getDamageValue()==1 && player.getInventory().countItem(Items.PAPER)==1,"Live route spends actual note materials");
        data.update(player.getUUID(),bound->0,k->{for(var a:TCAspects.catalog().all())k.setPool(a,(short)100);return null;});
        difficulty[0]=0;
        var purchase=book.request(player,book.token(),2,definition.key());
        test.assertTrue(purchase instanceof ServerResearchBook.Purchase p && p.result().accepted(),"Switching back uses purchase even when an older note is carried");
        test.assertTrue(ink.getDamageValue()==1 && player.getInventory().countItem(Items.PAPER)==1 && player.getInventory().countItem(TCItems.RESEARCH_NOTES.get())==1,"Purchase preserves the old note and paper/ink");
        for(var cost:definition.aspects().entrySet())test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().pool(TCAspects.catalog().require(cost.getKey()))==100-cost.getValue(),"Live purchase exact points");
        book.close();test.assertTrue(book.request(player,book.token(),3,definition.key()) instanceof ServerResearchBook.Denied,"Explicit close revokes subsequent requests");
        tc4revival.TC4Revival.LOGGER.info("PASS openBookUsesLiveDifficultyForSecondaryResearch: {} purchase-to-note-to-purchase in one session",definition.key());test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void bookSessionRoutesDifficultyAndDebitsActualResourcesOnce(GameTestHelper test) {
        var definition=ResearchCatalogs.catalog().require("THAUMIUM");
        for(int difficulty=-1;difficulty<=1;difficulty++) {
            final int mode=difficulty;
            var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookTransaction"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.THAUMONOMICON.get()));
            var data=KnowledgeSavedData.get(player.server);
            var book=new ServerResearchBook(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false,data,()->new ServerResearchBook.Settings(mode,false,false));
            data.update(player.getUUID(),bound->0,k->{k.wipe();k.rememberResearch("@THAUMIUM");for(String parent:definition.parents())k.rememberResearch(parent);for(String parent:definition.hiddenParents())k.rememberResearch(parent);for(var a:TCAspects.catalog().all())k.setPool(a,(short)0);return null;});
            test.assertTrue(book.request(player,UUID.randomUUID(),0,"THAUMIUM") instanceof ServerResearchBook.Denied,"Wrong opening token rejected");
            long revision=data.revision(player.getUUID());var failed=book.request(player,book.token(),0,"THAUMIUM");
            test.assertTrue(data.revision(player.getUUID())==revision,"Missing resources do not commit knowledge");
            test.assertTrue(book.request(player,book.token(),0,"THAUMIUM") instanceof ServerResearchBook.Denied,"Failed gameplay request still consumes sequence");
            var ink=new ItemStack(TCItems.SCRIBING_TOOLS.get());ink.setDamageValue(ink.getMaxDamage()-1);
            player.getInventory().setItem(1,ink);player.getInventory().setItem(2,new ItemStack(Items.PAPER,2));
            data.update(player.getUUID(),bound->0,k->{for(var a:TCAspects.catalog().all())k.setPool(a,(short)100);return null;});
            var outcome=book.request(player,book.token(),1,"THAUMIUM");
            if(mode==-1) {
                test.assertTrue(failed instanceof ServerResearchBook.Purchase p && p.result().rejection().orElseThrow()==ResearchBookActions.Rejection.POINTS,"Easy mode rejects insufficient points");
                test.assertTrue(outcome instanceof ServerResearchBook.Purchase p && p.result().accepted(),"Easy mode buys research instead of note");
                var knowledge=data.snapshot(player.getUUID()).orElseThrow();
                test.assertTrue(knowledge.researchMarkers().contains("THAUMIUM") && player.getInventory().countItem(Items.PAPER)==2 && ScribingToolsItem.ink(ink)==1,"Purchase does not spend paper/ink");
                for(var cost:definition.aspects().entrySet())test.assertTrue(knowledge.pool(TCAspects.catalog().require(cost.getKey()))==100-cost.getValue(),"Purchase pays exact research points");
            } else {
                test.assertTrue(failed instanceof ServerResearchBook.Note n && n.result().status()==ResearchBookInventory.Status.INK,"Note route validates actual scribing tool");
                test.assertTrue(outcome instanceof ServerResearchBook.Note n && n.result().status()==ResearchBookInventory.Status.CREATED,"Normal/hard primary research creates note");
                test.assertTrue(player.getInventory().countItem(Items.PAPER)==1 && ScribingToolsItem.ink(ink)==0 && player.getInventory().countItem(TCItems.RESEARCH_NOTES.get())==1,"Creates one note, spends one paper and final ink, keeps exhausted tool");
                var note=player.getInventory().items.stream().filter(s->s.is(TCItems.RESEARCH_NOTES.get())).findFirst().orElseThrow();
                test.assertTrue(ResearchNoteNbt.read(note.getTag(),TCAspects.catalog()).note().orElseThrow().key().equals("THAUMIUM"),"Actual generated note has selected key");
                test.assertTrue(!data.snapshot(player.getUUID()).orElseThrow().researchMarkers().contains("THAUMIUM"),"Receiving note does not complete research");
                var repeated=book.request(player,book.token(),2,"THAUMIUM");
                test.assertTrue(repeated instanceof ServerResearchBook.Note n && n.result().status()==ResearchBookInventory.Status.EXISTING && player.getInventory().countItem(Items.PAPER)==1,"Existing inventory note wins before exhausted-ink check, no extra cost");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            test.assertTrue(book.request(player,book.token(),3,"THAUMIUM") instanceof ServerResearchBook.Denied && !book.valid(),"Replacing held book revokes the opening");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS bookSessionRoutesDifficultyAndDebitsActualResourcesOnce: three difficulties, token/replay, points versus paper/ink, existing note and held context");test.succeed();
    }
}
