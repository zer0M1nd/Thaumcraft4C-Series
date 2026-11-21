package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class BlockScanCoverageServerTests {
    @GameTest(template="wand_test_empty")
    public static void placedContentScanIdentitiesCoverAllStates(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));int states=0;
        var names=new ArrayList<String>(List.of("amber_block","arcane_bore","arcane_bore_base","arcane_ear","arcane_lamp","fertility_lamp","growth_lamp","levitator",
                "greatwood_log","silverwood_log","silverwood_node_log","greatwood_leaves","silverwood_leaves","greatwood_sapling","silverwood_sapling",
                "greatwood_planks","silverwood_planks","greatwood_slab","silverwood_slab","greatwood_stairs","silverwood_stairs","shimmerleaf","cinderpearl","vishroom","mana_pod"));
        for(String element:List.of("air","fire","water","earth","order","entropy","mixed"))names.add(element+"_crystal_cluster");
        for(String name:names) {
            var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:"+name));
            String itemName=name.equals("silverwood_node_log")?"silverwood_log":name.equals("mana_pod")?"mana_bean":name;
            var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+itemName));var stack=new ItemStack(item);
            var expectedKey=LegacyItemMappings.identity().recipeKey("thaumcraft:"+itemName);
            var expected=ItemStackAspects.resolve(stack).orElseGet(tc4revival.aspect.AspectAmounts::new);
            for(var state:block.getStateDefinition().getPossibleStates()) {
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
                test.assertTrue(level.getBlockState(pos).equals(state),"Fixture retains state "+state);
                var mapping=BlockScanTargets.target(state).orElseThrow();test.assertTrue(mapping.legacyKey().equals(expectedKey),"Picked state identity matches its old item form: "+state);
                var target=ScanTargetResolver.block(level,pos).orElseThrow();
                if(level.getBlockEntity(pos) instanceof ScannableNode node) {
                    test.assertTrue(target.kind()==PlayerKnowledge.ScanKind.PHENOMENON&&target.identity().orElseThrow().equals("NODE"+node.nodeId()),"Node-bearing silverwood keeps node priority");
                } else {
                    test.assertTrue(target.identity().orElseThrow().equals(Integer.toString(LegacyItemMappings.identity().scanHash(expectedKey))),"Placed state has old scan hash: "+state);
                    test.assertTrue(target.aspects().view().equals(expected.view()),"Placed state has the plain picked item's ordered aspect values: "+state);
                }
                states++;
            }
        }
        for(var block:ForgeRegistries.BLOCKS) {
            var id=ForgeRegistries.BLOCKS.getKey(block);
            if(!id.getNamespace().equals("thaumcraft")||block.asItem()==Items.AIR||block==TCArcane.NODE_JAR.get()||block instanceof AuraNodeBlock||block instanceof EnergizedNodeBlock)continue;
            if(LegacyItemMappings.identity().recipeKey(ForgeRegistries.ITEMS.getKey(block.asItem()).toString())==null)continue;
            for(var state:block.getStateDefinition().getPossibleStates())test.assertTrue(BlockScanTargets.target(state).isPresent(),"All registered old item-bearing blocks have a scan rule: "+state);
        }
        test.assertTrue(BlockScanTargets.target(Blocks.CHERRY_PLANKS.defaultBlockState()).isPresent(),"Bundled modern vanilla table supplies cherry scan identity");
        tc4revival.TC4Revival.LOGGER.info("PASS placedContentScanIdentitiesCoverAllStates: {} explicit families, {} states plus complete item-bearing block coverage",names.size(),states);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void wardedBlocksRetainScanIdentityValuesAndNames(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WardScanAudit"));
        var states=new ArrayList<net.minecraft.world.level.block.state.BlockState>();
        states.addAll(Blocks.OAK_LOG.getStateDefinition().getPossibleStates());states.addAll(Blocks.DIAMOND_BLOCK.getStateDefinition().getPossibleStates());
        states.addAll(TCArcane.GREATWOOD_LOG.get().getStateDefinition().getPossibleStates());states.addAll(TCArcane.ARCANE_STONE_BRICKS.get().getStateDefinition().getPossibleStates());
        for(var state:states) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
            var before=ScanTargetResolver.block(level,pos).orElseThrow();
            player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-3);player.setYRot(0);player.setXRot(0);
            var selectedBefore=ScanTargetSelector.select(player).orElseThrow();
            test.assertTrue(WardedBlockEntity.protect(player,pos),"Actual ward protection accepts fixture "+state);
            var after=ScanTargetResolver.block(level,pos).orElseThrow();var selectedAfter=ScanTargetSelector.select(player).orElseThrow();
            test.assertTrue(before.continuity().equals(after.continuity())&&before.identity().equals(after.identity())&&before.aspects().view().equals(after.aspects().view()),"Ward preserves old picked identity and values: "+state);
            test.assertTrue(selectedBefore.name().equals(selectedAfter.name())&&selectedAfter.target().identity().equals(before.identity()),"Actual targeting displays the protected item's name and identity");
            test.assertTrue(after.clue().isPresent(),"Ward retains the original research clue source");
            var ward=(WardedBlockEntity)level.getBlockEntity(pos);var saved=ward.saveWithoutMetadata();ward.load(saved);
            test.assertTrue(ScanTargetResolver.block(level,pos).orElseThrow().identity().equals(before.identity()),"Persisted ward retains scan identity");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS wardedBlocksRetainScanIdentityValuesAndNames: {} original states",states.size());test.succeed();
    }
}
