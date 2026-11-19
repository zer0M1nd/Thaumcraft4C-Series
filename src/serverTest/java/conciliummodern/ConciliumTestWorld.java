package conciliummodern;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

/** Wait for actual entity sections; loading terrain alone does not start world entity ticks. */
final class ConciliumTestWorld {
    static void ready(GameTestHelper test,List<BlockPos> positions,Runnable action){
        var absolute=positions.stream().map(test::absolutePos).map(BlockPos::immutable).toList();
        for(var pos:absolute)test.getLevel().getChunkAt(pos);
        test.runAfterDelay(1,()->await(test,absolute,action,1));
    }
    private static void await(GameTestHelper test,List<BlockPos> positions,Runnable action,int elapsed){
        var level=test.getLevel();
        var pending=positions.stream().filter(pos->!level.areEntitiesLoaded(new ChunkPos(pos).toLong())||!level.isPositionEntityTicking(pos)).toList();
        if(pending.isEmpty()){
            com.mojang.logging.LogUtils.getLogger().info("CONCILIUM_FIXTURE_READY waited={} chunks={}",elapsed,positions.stream().map(ChunkPos::new).distinct().toList());
            action.run();return;
        }
        if(elapsed>=180){
            test.fail("Entity sections never became ready: "+pending.stream().map(pos->{var chunk=new ChunkPos(pos);return pos+" chunk="+chunk+" loaded="+level.areEntitiesLoaded(chunk.toLong())+" ticking="+level.isPositionEntityTicking(pos)+" forced="+level.getForcedChunks().contains(chunk.toLong());}).toList());return;
        }
        test.runAfterDelay(1,()->await(test,positions,action,elapsed+1));
    }
    private ConciliumTestWorld(){}
}
