package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.editor.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.sync.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class AspectEditorServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=1200)
    public static void aspectEditorSavesReloadsAndSynchronizes(GameTestHelper t)throws Exception{
        var server=t.getLevel().getServer();var source=server.createCommandSourceStack();
        var command=server.getCommands().getDispatcher().getRoot().getChild("thaumcraft").getChild("aspecteditor");
        t.assertTrue(!command.canUse(source.withPermission(0))&&command.canUse(source.withPermission(2)),"Editor command requires operator level 2");
        t.assertTrue(!AspectEditorServer.authorized(null),"Missing sender is rejected");
        var previous=new ArrayList<>(server.getPackRepository().getSelectedIds());
        var pig=net.minecraft.world.entity.EntityType.PIG.create(t.getLevel());
        var pigBefore=tc4revival.aspect.entity.EntityAspects.resolve(pig).orElseThrow();
        var root=server.getWorldPath(LevelResource.DATAPACK_DIR);
        var first=EditorPackStore.write(root,"first_test","TC4Tester",new EditorDefinitions(Map.of("item:minecraft:ender_pearl",Map.of("aer",12)),0),EditorResources.server().order());
        var second=EditorPackStore.write(root,"second_test","TC4Tester",new EditorDefinitions(Map.of(
                "item:minecraft:ender_pearl",Map.of(),"block:minecraft:end_portal",Map.of("aer",127),
                "item:minecraft:sculk_catalyst",Map.of("ignis",19),"item:minecraft:brown_mushroom_block",Map.of("aer",88),
                "entity:minecraft:pig",Map.of("aer",37),"entity:minecraft:cow",Map.of()),0),first.order());
        server.getPackRepository().reload();var selected=new ArrayList<>(previous);
        // Reverse enable order deliberately: actual save sequence must still win.
        selected.add(second.packId());selected.add(first.packId());
        server.reloadResources(selected).whenComplete((unused,error)->server.execute(()->{
            try{
                if(error!=null)throw new IllegalStateException(error);
                var pearl=new ItemStack(Items.ENDER_PEARL);
                t.assertTrue(ItemStackAspects.resolve(pearl).orElseThrow().isEmpty(),"Saved explicit-empty wins; recipes cannot resurrect it");
                var data=new AspectCalculationData(ServerAspectData.snapshot(),TCAspects.catalog(),(id,tag)->ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)).builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(tag))));
                t.assertTrue(ItemStackAspects.resolve(pearl,data).orElseThrow().isEmpty(),"Remote-client evaluator shares saved empty definition");
                var pos=t.absolutePos(new BlockPos(1,2,1));t.getLevel().setBlock(pos,Blocks.END_PORTAL.defaultBlockState(),2);
                t.assertTrue(tc4revival.research.ScanTargetResolver.block(t.getLevel(),pos).orElseThrow().aspects().amount(TCAspects.catalog().require("aer"))==127,"No-item block override reaches real scanning without legacy cap");
                t.assertTrue(data.editor().get("block:minecraft:end_portal").orElseThrow().get("aer")==127,"Block definition synchronizes to client");
                t.assertTrue(!Blocks.END_PORTAL.defaultBlockState().isAir()&&Blocks.END_PORTAL.asItem()==Items.AIR,"End portal is included by technical-block enumeration");
                var fallbackPos=t.absolutePos(new BlockPos(2,2,1));t.getLevel().setBlock(fallbackPos,Blocks.SCULK_CATALYST.defaultBlockState(),2);
                t.assertTrue(BlockScanTargets.target(Blocks.SCULK_CATALYST.defaultBlockState()).isEmpty(),"Test block exercises unmapped fallback");
                var blockScan=tc4revival.research.ScanTargetResolver.block(t.getLevel(),fallbackPos).orElseThrow();
                var itemScan=tc4revival.research.ScanTargetResolver.inventory(new ItemStack(Items.SCULK_CATALYST)).orElseThrow();
                t.assertTrue(blockScan.aspects().view().equals(itemScan.aspects().view())&&blockScan.aspects().amount(TCAspects.catalog().require("ignis"))==19
                        &&blockScan.identity().equals(itemScan.identity()),"Unmapped block uses item scan amounts and identity");
                var blockClue=(tc4revival.research.ResearchRules.ItemClue)blockScan.clue().orElseThrow();
                var itemClue=(tc4revival.research.ResearchRules.ItemClue)itemScan.clue().orElseThrow();
                for(var research:tc4revival.research.ResearchCatalogs.catalog().all())for(var trigger:research.triggers().items().orElse(List.of()))
                    t.assertTrue(blockClue.matches().test(trigger)==itemClue.matches().test(trigger),"Fallback clue matches item clue behavior: "+research.key());
                t.getLevel().setBlock(fallbackPos,Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(),2);
                t.assertTrue(tc4revival.research.ScanTargetResolver.block(t.getLevel(),fallbackPos).orElseThrow().aspects().view()
                        .equals(ItemStackAspects.resolve(new ItemStack(Items.BROWN_MUSHROOM)).orElseThrow().view()),"Existing picked-item mapping wins over default fallback");
                var pigAfter=tc4revival.aspect.entity.EntityAspects.resolve(pig).orElseThrow();
                t.assertTrue(pigAfter.legacyIdentity().equals(pigBefore.legacyIdentity())&&EditorDefinitions.values(pigAfter.aspects()).equals(Map.of("aer",37)),"Entity edit changes values but retains scan identity");
                t.assertTrue(tc4revival.research.ScanTargetResolver.entity(pig).orElseThrow().aspects().amount(TCAspects.catalog().require("aer"))==37,"Entity scan sees saved definition");
                var cow=net.minecraft.world.entity.EntityType.COW.create(t.getLevel());
                t.assertTrue(tc4revival.research.ScanTargetResolver.entity(cow).orElseThrow().aspects().isEmpty(),"Explicit empty entity overrides base and contributions");
                t.assertTrue(data.editor().get("entity:minecraft:pig").orElseThrow().get("aer")==37
                        &&data.entities().preview("minecraft:pig").orElseThrow().view().equals(pigBefore.aspects().view()),"Client receives entity draft override and baseline definitions");
                t.assertTrue(server.getPackRepository().getSelectedIds().contains(second.packId()),"New save enabled in world");
                server.reloadResources(previous).whenComplete((u,restore)->server.execute(()->t.runAfterDelay(1,()->{
                    if(restore!=null)t.fail("Restore failed: "+restore);
                    else {archiveFixturePacks(t, first, second);
                        t.assertTrue(!ItemStackAspects.resolve(pearl).orElseThrow().isEmpty(),"Disabling editor packs restores baseline and resets caches");
                        t.assertTrue(tc4revival.aspect.entity.EntityAspects.resolve(pig).orElseThrow().aspects().view().equals(pigBefore.aspects().view()),"Disabling pack restores entity defaults");
                        pass("save-reload-sync-block-fallback-entities");t.succeed();}
                })));
            }catch(Exception failure){
                tc4revival.TC4Revival.LOGGER.error("TC_EDITOR_FAIL",failure);
                server.reloadResources(previous).whenComplete((u,restore)->server.execute(()->t.runAfterDelay(1,()->{
                    if(restore==null)archiveFixturePacks(t, first, second);
                    t.fail("Editor integration failed: "+failure);
                })));
            }
        }));
    }
    private static void archiveFixturePacks(GameTestHelper test, EditorPackStore.Saved... packs) {
        // A future world copy must not discover and enable our explicit-empty
        // cow/pearl definitions as new packs. Retain evidence outside datapacks.
        try {
            var archive=packs[0].directory().getParent().resolveSibling("validation-editor-pack-archive");
            java.nio.file.Files.createDirectories(archive);
            for(var pack:packs)if(java.nio.file.Files.exists(pack.directory()))
                java.nio.file.Files.move(pack.directory(),archive.resolve(pack.directory().getFileName()+"-"+UUID.randomUUID()));
        } catch(java.io.IOException failure) {
            test.fail("Could not archive editor fixture packs: "+failure);
        }
    }
    private static void pass(String value){tc4revival.TC4Revival.LOGGER.info("TC_EDITOR_PASS {}",value);}
}
