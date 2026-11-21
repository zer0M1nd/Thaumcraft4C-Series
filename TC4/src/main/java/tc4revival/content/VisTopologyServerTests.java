package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisTopologyServerTests {
    private static EnergizedNodeBlockEntity source(GameTestHelper test,BlockPos pos){
        var level=test.getLevel();level.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);
        var source=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);
        source.configure(new AspectAmounts().add(TCAspects.catalog().require("aer"),10000),AuraNodeType.NORMAL,null,"topology");source.tick();return source;
    }
    private static VisRelayBlockEntity relay(GameTestHelper test,BlockPos pos){
        test.getLevel().setBlock(pos,TCArcane.VIS_RELAY.get().defaultBlockState(),2);
        return (VisRelayBlockEntity)test.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="wand_test_empty")
    public static void visRelayAllAttunementsAndRangeBoundaries(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var source=source(test,pos);var first=relay(test,pos.east(6));var second=relay(test,pos.east(12));
        try {
            for(int a=-1;a<6;a++)for(int b=-1;b<6;b++){
                first.setAttunement(a);second.setAttunement(b);source.tickNetwork();first.tickNetwork();second.tickNetwork();
                test.assertTrue(first.parent()==source,"Uncolored source accepts every relay attunement");
                test.assertTrue((second.parent()==first)==(a==-1||b==-1||a==b),"Old color compatibility "+a+"/"+b+" first="+first.getBlockPos()+" second="+second.getBlockPos()+" live="+VisNetwork.live(first)+"/"+VisNetwork.live(second)+" visible="+VisNetwork.visible(first,second)+" parent="+(second.networkParent()==null?null:second.networkParent().key()));
                var saved=second.saveWithoutMetadata();var restored=new VisRelayBlockEntity(second.getBlockPos(),second.getBlockState());restored.load(saved);
                test.assertTrue(restored.attunement()==b&&!saved.contains("Parent"),"Attunement persists, topology is rebuilt");
            }
            level.setBlock(second.getBlockPos(),Blocks.AIR.defaultBlockState(),2);level.setBlock(first.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            var boundary=relay(test,pos.east(8));boundary.tickNetwork();test.assertTrue(boundary.parent()==source,"Exactly eight blocks connects");
            level.setBlock(boundary.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            var outside=relay(test,pos.east(9));outside.tickNetwork();test.assertTrue(outside.parent()==null,"Nine blocks cannot connect");
            level.setBlock(outside.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            tc4revival.TC4Revival.LOGGER.info("PASS visRelayAllAttunementsAndRangeBoundaries: 49 color pairs, color NBT and eight/nine-block range");test.succeed();
        }finally{for(int x:new int[]{0,6,8,9,12})level.setBlock(pos.east(x),Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty")
    public static void visTopologyOcclusionReconnectAndConsumerSelection(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var air=TCAspects.catalog().require("aer");
        var source=source(test,pos);var a=relay(test,pos.east(6));var b=relay(test,pos.east(12));var c=relay(test,pos.east(18));
        try {
            a.tickNetwork();b.tickNetwork();c.tickNetwork();
            test.assertTrue(a.parent()==source&&b.parent()==a&&c.parent()==b&&c.root()==source,"Three relays form a live source tree");
            test.assertTrue(VisNetwork.drain(level,pos.east(22),air,30)==30&&a.pulsing()&&b.pulsing()&&c.pulsing(),"Remote consumer drains through full chain and pulses ancestors");
            level.setBlock(pos.north(),Blocks.STONE.defaultBlockState(),2);
            test.assertTrue(VisNetwork.drain(level,pos.north(3),air,10)==10,"Consumer supply ignores intervening solid wall as in old network");
            level.setBlock(pos.north(),Blocks.AIR.defaultBlockState(),2);
            test.assertTrue(VisNetwork.drain(level,pos,air,10)==10,"Consumer colocated with source can use a distinct nearby relay");
            level.setBlock(pos.east(3),Blocks.STONE.defaultBlockState(),2);source.networkTicks=0;source.tickNetwork();a.tickNetwork();b.tickNetwork();c.tickNetwork();
            test.assertTrue(a.root()==null&&b.root()==null&&c.root()==null&&VisNetwork.drain(level,pos.east(22),air,1)==0,"Opaque obstruction detaches the full downstream tree");
            level.setBlock(pos.east(3),Blocks.AIR.defaultBlockState(),2);
            for(var node:new VisRelayBlockEntity[]{a,b,c}){node.networkTicks=0;node.tickNetwork();}
            test.assertTrue(c.root()==source,"Removing obstruction reconnects in parent-first refresh order");
            var preferred=relay(test,pos.north(4));preferred.tickNetwork();test.assertTrue(preferred.parent()==source,"Direct source selection remains available");
            level.setBlock(preferred.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(a.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            test.assertTrue(b.parent()==null&&c.parent()==null&&c.consumeVis(air,1)==0,"Actual relay removal immediately invalidates descendants: b="+(b.parent()==null?"null":b.parent().getBlockPos())+", c="+(c.parent()==null?"null":c.parent().getBlockPos())+", aLive="+VisNetwork.live(a)+", root="+(c.root()==null?"null":c.root().getBlockPos()));
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            test.assertTrue(VisNetwork.drain(level,pos.north(3),air,10)==0,"Removed source cannot remain a supply root");
            tc4revival.TC4Revival.LOGGER.info("PASS visTopologyOcclusionReconnectAndConsumerSelection: chain, pulses, consumer wall, obstruction refresh, reconnect and removal");test.succeed();
        }finally{for(int x:new int[]{0,3,6,12,18})level.setBlock(pos.east(x),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.north(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.north(4),Blocks.AIR.defaultBlockState(),2);}
    }
}
