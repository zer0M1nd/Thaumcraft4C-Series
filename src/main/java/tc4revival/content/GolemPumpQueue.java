package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/** Stable descending distance order over the original depth-first, 26-neighbor fluid walk. */
public  class GolemPumpQueue {
    private record Source(BlockPos pos,float distance) {}
    private static  class Frame { final BlockPos pos; int next; Frame(BlockPos pos) { this.pos=pos; } }
    private final Map<BlockPos,ArrayDeque<Source>> queues=new HashMap<>();
    public BlockPos next(ServerLevel level,BlockPos origin,Fluid fluid,float range) {
        var queue=queues.get(origin);
        if (queue==null || queue.isEmpty()) { queue=rebuild(level,origin,fluid,range); queues.put(origin,queue); }
        while (!queue.isEmpty()) {
            var next=queue.removeFirst().pos();
            if (GolemLiquidSources.source(level,next,fluid)) return next;
        }
        return GolemLiquidSources.source(level,origin,fluid)?origin:null;
    }
    private ArrayDeque<Source> rebuild(ServerLevel level,BlockPos origin,Fluid fluid,float range) {
        var visited=new HashSet<BlockPos>(); var traversal=new ArrayDeque<Frame>(); var result=new ArrayList<Source>();
        visited.add(origin); traversal.push(new Frame(origin)); float squared=range*range;
        while (!traversal.isEmpty()) {
            var frame=traversal.peek();
            if (frame.next>=27) { traversal.pop(); continue; }
            int index=frame.next++;
            int x=index/9-1,y=index/3%3-1,z=index%3-1;
            if (x==0 && y==0 && z==0) continue;
            var pos=frame.pos.offset(x,y,z); float distance=(float)pos.distSqr(origin);
            if (distance>squared || !visited.add(pos) || !GolemLiquidSources.connected(level,pos,fluid)) continue;
            if (GolemLiquidSources.source(level,pos,fluid)) result.add(new Source(pos,distance));
            traversal.push(new Frame(pos));
        }
        result.sort((a,b)->Float.compare(b.distance(),a.distance()));
        return new ArrayDeque<>(result);
    }
}
