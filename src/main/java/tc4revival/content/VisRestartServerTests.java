package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

/** Two explicitly invoked stages, separated by save-all flush, stop and a fresh server JVM. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VisRestartServerTests {
    private static final BlockPos ROOT=new BlockPos(36482,200,8002),A=ROOT.east(6),B=ROOT.east(12);
    private static final String ID="vis-network-disk-audit-20260911";
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void prepareVisNetworkDiskRestart(GameTestHelper test){
        var level=test.getLevel();var chunk=new ChunkPos(ROOT);
        test.assertTrue(!level.getForcedChunks().contains(chunk.toLong()),"Restart fixture must own a previously unforced chunk");
        for(var pos:new BlockPos[]{ROOT,A,B,A.below(),B.below()})test.assertTrue(level.getBlockState(pos).isAir(),"Restart fixture refuses to overwrite existing block at "+pos);
        level.setChunkForced(chunk.x,chunk.z,true);
        level.setBlock(ROOT,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);var source=(EnergizedNodeBlockEntity)level.getBlockEntity(ROOT);
        source.configure(new AspectAmounts().add(TCAspects.catalog().require("aer"),144),AuraNodeType.NORMAL,null,ID);
        for(var pos:new BlockPos[]{A,B}){level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,TCArcane.VIS_RELAY.get().defaultBlockState(),2);((VisRelayBlockEntity)level.getBlockEntity(pos)).setAttunement(3);}
        test.runAfterDelay(65,()->{
            test.assertTrue(((VisRelayBlockEntity)level.getBlockEntity(B)).root()==source,"Prepared network naturally connects before saving");
            tc4revival.TC4Revival.LOGGER.info("PASS prepareVisNetworkDiskRestart: owned forced chunk {}, source {}, two color-3 relays; SAVE AND RESTART before verification",chunk,ID);test.succeed();
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void verifyVisNetworkAfterDiskRestart(GameTestHelper test){
        var level=test.getLevel();var chunk=new ChunkPos(ROOT);
        test.assertTrue(level.getForcedChunks().contains(chunk.toLong()),"Owned forced chunk persisted to disk");
        test.assertTrue(level.getBlockEntity(ROOT) instanceof EnergizedNodeBlockEntity source&&source.nodeId().equals(ID),"Correct prepared source restored from disk");
        test.runAfterDelay(65,()->{
            try {
                var source=(EnergizedNodeBlockEntity)level.getBlockEntity(ROOT);var a=(VisRelayBlockEntity)level.getBlockEntity(A);var b=(VisRelayBlockEntity)level.getBlockEntity(B);
                test.assertTrue(a.attunement()==3&&b.attunement()==3&&a.parent()==source&&b.parent()==a,"Disk-restored colors and natural topology refer to new process entities");
                var air=TCAspects.catalog().require("aer");test.assertTrue(source.aura().amount(air)==144&&source.supply().amount(air)==12,"Source aura and derived supply survive restart");
                test.assertTrue(VisNetwork.drain(level,B.east(2),air,12)==12&&VisNetwork.drain(level,B.east(2),air,1)==0,"Reloaded chain provides exactly one world-tick budget");
                tc4revival.TC4Revival.LOGGER.info("PASS verifyVisNetworkAfterDiskRestart: disk source/color restore, natural two-relay reconstruction, exact supply and fixture cleanup");test.succeed();
            }finally{
                for(var pos:new BlockPos[]{ROOT,A,B,A.below(),B.below()})level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                level.setChunkForced(chunk.x,chunk.z,false);
            }
        });
    }
}
