package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class NodeConversionServerTests {
    @GameTest(template="wand_test_empty")
    public static void advancedNodeStabilizerInfusionIsObtainable(GameTestHelper test) {
        var level=test.getLevel();var center=new net.minecraft.world.item.ItemStack(TCItems.NODE_STABILIZER.get());
        var components=new ArrayList<net.minecraft.world.item.ItemStack>();
        for(int repeat=0;repeat<2;repeat++)for(var item:List.of(TCItems.NITOR.get(),net.minecraft.world.item.Items.REDSTONE_BLOCK,TCItems.ALUMENTUM.get(),net.minecraft.world.item.Items.REDSTONE_BLOCK))
            components.add(new net.minecraft.world.item.ItemStack(item));
        var input=new InfusionRecipe.Input(center,components,key->key.equals("NODESTABILIZERADV"));
        var recipe=InfusionRecipe.find(level,input).orElseThrow(()->new AssertionError("Advanced stabilizer has a loaded, usable infusion recipe"));
        test.assertTrue(recipe.getId().equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/advanced_node_stabilizer")),"Correct recipe selected through real recipe manager");
        var expected=new AspectAmounts().add(TCAspects.catalog().require("auram"),32).add(TCAspects.catalog().require("praecantatio"),16)
                .add(TCAspects.catalog().require("ordo"),16).add(TCAspects.catalog().require("potentia"),16);
        var plan=recipe.plan(center);
        test.assertTrue(plan.instability()==10&&plan.aspects().view().equals(expected.view())&&plan.components().size()==8,"Old instability, four costs and eight pedestal entries");
        var output=recipe.assemble(input,level.registryAccess());
        test.assertTrue(output.is(TCItems.ADVANCED_NODE_STABILIZER.get())&&output.getCount()==1,"Infusion produces one advanced stabilizer");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components,key->false),level),"Research is required");
        Collections.reverse(components);test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,components,key->true),level),"Pedestal ordering may differ");
        components.remove(0);test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components,key->true),level),"Missing even one repeated component prevents infusion");
        var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlock(pos,TCArcane.NODE_STABILIZER.get().defaultBlockState(),18);
        test.assertTrue(((NodeStabilizerBlockEntity)level.getBlockEntity(pos)).lockStrength()==1,"Original stabilizer lock is one");
        level.setBlock(pos,TCArcane.ADVANCED_NODE_STABILIZER.get().defaultBlockState(),18);
        test.assertTrue(((NodeStabilizerBlockEntity)level.getBlockEntity(pos)).lockStrength()==2,"Obtainable upgraded device exposes advanced lock two");
        tc4revival.TC4Revival.LOGGER.info("PASS advancedNodeStabilizerInfusionIsObtainable: loaded recipe, research, components, cost, result and lock strengths");test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void nodeConversionCyclesPreserveCapacityAndTiming(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));var converterPos=pos.above();
        var air=TCAspects.catalog().require("aer");var light=TCAspects.catalog().require("lux");var earth=TCAspects.catalog().require("terra");
        var values=new AspectAmounts().add(air,100).add(light,64).add(earth,25);int cases=0;
        for(var type:AuraNodeType.values())for(var modifier:Arrays.asList(null,AuraNodeModifier.BRIGHT,AuraNodeModifier.PALE,AuraNodeModifier.FADING)) {
            level.setBlock(converterPos.east(),Blocks.AIR.defaultBlockState(),18);
            level.setBlock(converterPos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            level.setBlock(pos.below(),TCArcane.NODE_STABILIZER.get().defaultBlockState(),18);
            level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),18);
            var ordinary=(AuraNodeBlockEntity)level.getBlockEntity(pos);ordinary.configure(values,type,modifier,"before-conversion",false);
            level.setBlock(converterPos,TCArcane.NODE_CONVERTER.get().defaultBlockState(),18);
            var converter=(NodeConverterBlockEntity)level.getBlockEntity(converterPos);
            converter.checkStatus();test.assertTrue(converter.status()==0&&converter.count()==0,"No power means idle");
            level.setBlock(converterPos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),18);
            level.updateNeighborsAt(converterPos.east(),Blocks.REDSTONE_BLOCK);
            test.assertTrue(converter.status()==1&&converter.count()==0&&converter.hasStabilizer(),"Real power notification begins charging");
            converter.tick();test.assertTrue(converter.count()==1&&ordinary.aspects().total()==188,"One charging tick takes one current vis, not capacity");
            for(int tick=1;tick<437;tick++)converter.tick();
            var saved=converter.saveWithoutMetadata();
            var restored=new NodeConverterBlockEntity(converterPos,converter.getBlockState());restored.load(saved);
            level.removeBlockEntity(converterPos);level.setBlockEntity(restored);converter=restored;
            test.assertTrue(converter.status()==1&&converter.count()==437,"In-progress conversion survives actual block entity replacement from NBT");
            long remaining=ordinary.aspects().total();
            level.setBlock(converterPos.east(),Blocks.AIR.defaultBlockState(),18);level.updateNeighborsAt(converterPos.east(),Blocks.AIR);
            converter.tick();test.assertTrue(converter.status()==0&&converter.count()==436&&ordinary.aspects().total()==remaining,"Power interruption unwinds progress without taking vis");
            level.setBlock(converterPos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),18);level.updateNeighborsAt(converterPos.east(),Blocks.REDSTONE_BLOCK);
            test.assertTrue(converter.status()==1&&converter.count()==436,"Restoring power resumes retained charge count");
            for(int tick=436;tick<1000;tick++)converter.tick();
            test.assertTrue(level.getBlockEntity(pos)==ordinary&&converter.count()==1000,"Threshold is reached before transformation on the next tick");
            converter.tick();
            test.assertTrue(level.getBlockEntity(pos) instanceof EnergizedNodeBlockEntity,"Full charge converts ordinary node");
            var energized=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);
            test.assertTrue(energized.aura().view().equals(values.view())&&energized.nodeType()==type&&energized.nodeModifier()==modifier,"Capacity/type/modifier survive conversion despite current charge consumption");
            test.assertTrue(energized.nodeId().equals("blank")&&converter.status()==2&&converter.count()==1000,"Old energized node identity and powered steady state");
            var copy=new EnergizedNodeBlockEntity(pos,energized.getBlockState());copy.load(energized.getUpdateTag());
            test.assertTrue(copy.aura().view().equals(energized.aura().view())&&copy.supply().view().equals(energized.supply().view())&&copy.nodeType()==type&&copy.nodeModifier()==modifier,"Full update NBT preserves both original aura and reduced supply");
            for(int tick=0;tick<5;tick++)converter.tick();test.assertTrue(converter.count()==1000,"Power holds energized state");
            level.setBlock(converterPos.east(),Blocks.AIR.defaultBlockState(),18);level.updateNeighborsAt(converterPos.east(),Blocks.AIR);
            for(int tick=0;tick<100;tick++)converter.tick();
            test.assertTrue(converter.count()==900,"Cooldown begins without power");
            level.updateNeighborsAt(converterPos.east(),Blocks.AIR);
            test.assertTrue(converter.status()==2&&converter.count()==1000,"Old neighbor notification resets an energized cooldown to one thousand");
            for(int tick=0;tick<950;tick++)converter.tick();
            test.assertTrue(converter.count()==50&&level.getBlockEntity(pos)==energized,"Cooldown retains energized node through count fifty");
            converter.tick();test.assertTrue(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity,"Following tick restores ordinary node");
            ordinary=(AuraNodeBlockEntity)level.getBlockEntity(pos);
            test.assertTrue(ordinary.base().view().equals(values.view())&&ordinary.aspects().total()==0&&ordinary.nodeType()==type&&ordinary.nodeModifier()==modifier,"De-energizing preserves capacity and clears all current charge");
            test.assertTrue(converter.status()==0&&converter.count()==49,"Conversion back enters idle and decrements the same tick");
            for(int tick=0;tick<55;tick++)converter.tick();test.assertTrue(converter.count()==0,"Idle count stops at zero");
            cases++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS nodeConversionCyclesPreserveCapacityAndTiming: {} type/modifier cycles",cases);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void energizedSupplyUsesLegacyReductionAndSharedTickBudget(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));
        level.setBlock(pos.below(),TCArcane.NODE_STABILIZER.get().defaultBlockState(),18);
        level.setBlock(pos.above(),TCArcane.NODE_CONVERTER.get().defaultBlockState(),18);
        level.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),18);
        var node=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);var catalog=TCAspects.catalog();
        var air=catalog.require("aer");var fire=catalog.require("ignis");var light=catalog.require("lux");var earth=catalog.require("terra");
        var aura=new AspectAmounts().add(air,100).add(light,64).add(earth,25);
        int[][] expected={{10,8,5},{10,8,5},{8,7,4},{7,5,3}};int row=0;
        for(var modifier:Arrays.asList(null,AuraNodeModifier.BRIGHT,AuraNodeModifier.PALE,AuraNodeModifier.FADING)) {
            node.configure(aura,AuraNodeType.NORMAL,modifier,"supply-audit");
            test.assertTrue(node.supply().size()==3&&node.supply().amount(air)==expected[row][0]&&node.supply().amount(fire)==expected[row][1]&&node.supply().amount(earth)==expected[row][2],"Maximum primal decomposition, float modifier before square root: "+modifier);
            test.assertTrue(node.consumeVis(air,3)==3&&node.consumeVis(air,100)==expected[row][0]-3&&node.consumeVis(air,1)==0,"Repeated consumers share one aspect budget within this tick");
            test.assertTrue(node.consumeVis(light,10)==0&&node.consumeVis(fire,-1)==0&&node.consumeVis(fire,100)==expected[row][1],"Compound/negative requests cannot drain and other primals keep independent budgets");
            row++;
        }
        node.configure(new AspectAmounts().add(air,67_108_863),AuraNodeType.NORMAL,null,"float-boundary");
        test.assertTrue(node.supply().amount(air)==8192,"Old float square root rounds the value immediately below a large square before flooring");
        node.configure(aura,AuraNodeType.UNSTABLE,null,"unstable-audit");
        for(int attempt=0;attempt<32;attempt++) {
            node.configure(aura,AuraNodeType.UNSTABLE,null,"unstable-audit");
            test.assertTrue(node.supply().amount(air)>=8&&node.supply().amount(air)<=12&&node.supply().amount(fire)>=6&&node.supply().amount(fire)<=10,"Unstable supply varies by minus two through plus two");
        }
        node.configure(aura,AuraNodeType.NORMAL,null,"budget-audit");node.tick();
        test.assertTrue(node.consumeVis(air,100)==10&&node.consumeVis(air,1)==0,"Budget exhausted before advancing the actual world tick");
        test.runAfterDelay(2,()->{
            test.assertTrue(node.consumeVis(air,100)==10&&node.consumeVis(air,1)==0,"A later world tick replenishes exactly one budget");
            test.assertTrue(node.supply().amount(air)==10&&node.aura().view().equals(aura.view()),"Consumption does not mutate displayed capacity or original aura");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            test.assertTrue(node.consumeVis(air,100)==0,"Detached old source cannot supply vis");
            tc4revival.TC4Revival.LOGGER.info("PASS energizedSupplyUsesLegacyReductionAndSharedTickBudget: modifiers, compound reduction, unstable range, float boundary, tick budget and removal");test.succeed();
        });
    }
}
