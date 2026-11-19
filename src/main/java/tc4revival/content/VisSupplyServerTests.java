package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisSupplyServerTests {
    private static EnergizedNodeBlockEntity source(GameTestHelper test,BlockPos pos){
        test.getLevel().setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);
        return (EnergizedNodeBlockEntity)test.getLevel().getBlockEntity(pos);
    }
    private static void supply(EnergizedNodeBlockEntity node,int amount){
        var aura=new AspectAmounts();for(var aspect:TCAspects.catalog().primals())aura.add(aspect,amount*amount);
        node.configure(aura,AuraNodeType.NORMAL,null,"supply-audit");node.tick();
    }
    @GameTest(template="wand_test_empty")
    public static void visMultipleSourcesConserveCombinedBudget(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var a=source(test,pos.north(3));var b=source(test,pos.south(3));var air=TCAspects.catalog().require("aer");
        try {
            for(int request:new int[]{0,1,7,8,18,19}){
                supply(a,7);supply(b,11);
                test.assertTrue(VisNetwork.drain(level,pos,TCAspects.catalog().require("fames"),10)==0,"Compound request rejected without debit");
                int drained=VisNetwork.drain(level,pos,air,request);
                test.assertTrue(drained==Math.min(18,request),"Multiple roots combine up to requested total");
                test.assertTrue(a.consumeVis(air,100)+b.consumeVis(air,100)==18-drained,"Exact combined source budget conserved independent of source ordering");
                test.assertTrue(VisNetwork.drain(level,pos,air,1)==0,"Repeated same-tick drain cannot duplicate budget");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS visMultipleSourcesConserveCombinedBudget: six request boundaries and source conservation");test.succeed();
        }finally{level.setBlock(a.getBlockPos(),Blocks.AIR.defaultBlockState(),2);level.setBlock(b.getBlockPos(),Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty")
    public static void visParentSelectionPreservesLegacySourceBias(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var root=source(test,pos);supply(root,10);
        var relayPos=pos.east(3).north();level.setBlock(relayPos,TCArcane.VIS_RELAY.get().defaultBlockState(),2);var relay=(VisRelayBlockEntity)level.getBlockEntity(relayPos);relay.tickNetwork();
        try {
            for(int x:new int[]{4,5}){
                var childPos=pos.east(x);level.setBlock(childPos,TCArcane.VIS_RELAY.get().defaultBlockState(),2);var child=(VisRelayBlockEntity)level.getBlockEntity(childPos);child.tickNetwork();
                test.assertTrue(child.parent()==(x==4?root:relay),"Source score distance squared minus sixteen competes with closer relay at x="+x);
                level.setBlock(childPos,Blocks.AIR.defaultBlockState(),2);
                root.tickNetwork();relay.tickNetwork();
            }
            tc4revival.TC4Revival.LOGGER.info("PASS visParentSelectionPreservesLegacySourceBias: source score 0 beats relay 2; source score 9 loses to relay 5");test.succeed();
        }finally{for(int x:new int[]{4,5})level.setBlock(pos.east(x),Blocks.AIR.defaultBlockState(),2);level.setBlock(relayPos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty")
    public static void workbenchChargerLimitsEachPrimalAndUsesOnlyWandSlot(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var node=source(test,pos.east(4));
        level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),2);var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        level.setBlock(pos.above(),TCArcane.WORKBENCH_CHARGER.get().defaultBlockState(),2);var charger=(WorkbenchChargerBlockEntity)level.getBlockEntity(pos.above());
        try {
            for(int room:new int[]{0,2,5,6,20}){
                supply(node,10);var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);int capacity=TCItems.CASTING_WAND.get().capacity(wand);
                for(var aspect:TCAspects.catalog().primals())wand.getOrCreateTag().putInt(aspect.tag(),capacity-room);
                table.setItem(10,wand);charger.tick();
                test.assertTrue(charger.root()==node,"Actual charger joins source network");
                for(var aspect:TCAspects.catalog().primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==capacity-room+Math.min(room,5),"First callback adds at most five centivis per primal");
                charger.tick();
                for(var aspect:TCAspects.catalog().primals()){
                    test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==capacity-room+Math.min(room,10),"Second callback stops at capacity or exhausted budget");
                    test.assertTrue(node.consumeVis(aspect,100)==10-Math.min(room,10),"Charger only drains charge actually stored");
                }
            }
            supply(node,10);var misplaced=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,false);table.setItem(10,new ItemStack(Items.DIRT));table.setItem(0,misplaced);charger.tick();
            for(var aspect:TCAspects.catalog().primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(misplaced,aspect)==0&&node.consumeVis(aspect,100)==10,"Only slot ten is a charge target; ordinary items consume nothing");
            tc4revival.TC4Revival.LOGGER.info("PASS workbenchChargerLimitsEachPrimalAndUsesOnlyWandSlot: five capacity boundaries, six primals, two callbacks and inventory targeting");test.succeed();
        }finally{table.clearContent();level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(node.getBlockPos(),Blocks.AIR.defaultBlockState(),2);}
    }
}
