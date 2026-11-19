package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Bounded connected-log search; never crosses an unloaded chunk or jumps a gap. */
public class ElementalTreeSearch {
    private ElementalTreeSearch() {}
    public static BlockPos furthest(Level level,BlockPos origin) {
        var tree=level.getBlockState(origin).getBlock();
        var visited=new java.util.HashSet<BlockPos>();
        var pending=new java.util.ArrayDeque<BlockPos>();
        visited.add(origin);pending.add(origin);
        var furthest=origin;double distance=0;
        while(!pending.isEmpty() && visited.size()<4096) {
            var current=pending.removeFirst();
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
                var candidate=current.offset(x,y,z);
                if(Math.abs(candidate.getX()-origin.getX())>24 || Math.abs(candidate.getY()-origin.getY())>48 || Math.abs(candidate.getZ()-origin.getZ())>24
                        || visited.contains(candidate) || level.isOutsideBuildHeight(candidate) || !level.hasChunkAt(candidate))continue;
                visited.add(candidate);
                var state=level.getBlockState(candidate);
                if(!LegacyWoodLogs.sameBlock(tree,state.getBlock()) || state.getDestroySpeed(level,candidate)<0)continue;
                pending.addLast(candidate);
                double squared=candidate.distSqr(origin);
                if(squared>distance) {distance=squared;furthest=candidate;}
            }
        }
        return furthest;
    }
}
