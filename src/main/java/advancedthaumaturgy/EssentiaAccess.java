package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import tc4revival.aspect.Aspect;
import tc4revival.content.EssentiaJarBlockEntity;
import thaumcraft.api.aspects.EssentiaStorageApi;

public class EssentiaAccess {
    /** Cache positions, never inventories; refresh on a bounded interval, never force chunks. */
    private final List<BlockPos> jars = new ArrayList<>();
    private long refreshAt;
    public int take(ServerLevel level, BlockPos origin, Aspect aspect, int radius, int height) {
        if (level.getGameTime() >= refreshAt || jars.isEmpty()) {
            jars.clear(); refreshAt = level.getGameTime() + 100;
            int cx0=(origin.getX()-radius)>>4,cx1=(origin.getX()+radius)>>4,cz0=(origin.getZ()-radius)>>4,cz1=(origin.getZ()+radius)>>4;
            for (int cx=cx0;cx<=cx1;cx++) for(int cz=cz0;cz<=cz1;cz++) {
                var chunk=level.getChunkSource().getChunkNow(cx,cz); if(chunk==null)continue;
                for(var e:chunk.getBlockEntities().entrySet()) {
                    var p=e.getKey();
                    if(e.getValue() instanceof EssentiaJarBlockEntity && Math.abs(p.getX()-origin.getX())<=radius && Math.abs(p.getZ()-origin.getZ())<=radius && Math.abs(p.getY()-origin.getY())<=height)jars.add(p.immutable());
                }
            }
            jars.sort(Comparator.comparingDouble(origin::distSqr));
        }
        for(var pos:jars) if(level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity) {
            int taken=EssentiaStorageApi.atInventory(level,pos).extract(aspect,1,false);
            if(taken==1){tc4revival.network.FocusEffectPacket.nodeZap(level,pos.getCenter(),origin.getCenter());return 1;}
        }
        return 0;
    }
}
