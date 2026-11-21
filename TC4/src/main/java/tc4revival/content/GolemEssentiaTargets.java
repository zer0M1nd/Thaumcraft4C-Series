package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Connected jars retain the original seven preference tiers, before distance chooses within a tier. */
public  class GolemEssentiaTargets {
    private GolemEssentiaTargets() {}
    private static String key(BlockPos pos) { return pos.getX()+":"+pos.getY()+":"+pos.getZ(); }
    private static double distance(GolemWorkHost host,BlockPos pos) { return Vec3.atCenterOf(pos).distanceToSqr(Vec3.atLowerCornerOf(host.home())); }
    public static BlockPos find(GolemWorkHost host) {
        if (!(host.mob().level() instanceof ServerLevel level) || host.essentia()==null || host.essentiaAmount()<=0) return null;
        var starts=new ArrayList<BlockPos>(); var selected=new ArrayList<BlockPos>();
        float range=host.configuration().range(); range*=range;
        for (var marker:host.markers().inDimension(level.dimension().location())) {
            if (!GolemEssentiaFilters.matches(host,host.essentia(),marker.color())) continue;
            var pos=marker.pos(); if (!level.hasChunkAt(pos) || distance(host,pos)>range) continue;
            var entity=level.getBlockEntity(pos);
            if (entity instanceof EssentiaJarBlockEntity) starts.add(pos);
            else if (entity instanceof EssentiaReservoirBlockEntity reservoir && accepts(host,reservoir,reservoir.facing())) selected.add(pos);
            else if (entity instanceof EssentiaTransport transport && accepts(host,transport,marker.face())) selected.add(pos);
            else if (!(entity instanceof EssentiaTransport)) {
                var storage=GolemEssentiaAccess.at(level,pos,marker.face(),true);
                if(storage!=null && storage.insert(host.essentia(),1,true)>0)selected.add(pos);
            }
        }
        var jars=new HashMap<String,EssentiaJarBlockEntity>();
        var forbidden=new HashSet<BlockPos>();
        for(var marker:host.markers().inDimension(level.dimension().location()))
            if(!GolemEssentiaFilters.matches(host,host.essentia(),marker.color()))forbidden.add(marker.pos());
        // A block may have several colored faces; one explicitly permitted mark is sufficient.
        for(var marker:host.markers().inDimension(level.dimension().location()))
            if(GolemEssentiaFilters.matches(host,host.essentia(),marker.color()))forbidden.remove(marker.pos());
        for (var start:starts) {
            var pending=new ArrayDeque<BlockPos>(); pending.push(start);
            while (!pending.isEmpty()) {
                var pos=pending.pop();
                if (forbidden.contains(pos) || jars.containsKey(key(pos)) || distance(host,pos)>range || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar)) continue;
                jars.put(key(pos),jar);
                // DFS in DOWN, UP, NORTH, SOUTH, WEST, EAST order without recursive stack overflow.
                for (int side=5;side>=0;side--) pending.push(pos.relative(Direction.from3DDataValue(side)));
            }
        }
        for (int tier=0;tier<7;tier++) {
            for (var jar:jars.values()) if (tier(host,jar)==tier) selected.add(jar.getBlockPos());
            if (!selected.isEmpty()) break;
        }
        BlockPos result=null; double best=Double.MAX_VALUE;
        for (var pos:selected) {
            double value=distance(host,pos);
            if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar && jar.voidJar()) value+=range;
            if (value<best) { best=value; result=pos; }
        }
        return result;
    }
    public static boolean accepts(GolemWorkHost host,EssentiaTransport transport,Direction face) {
        return transport.canInput(face)
                && (transport.suctionType(face)==null || transport.suctionType(face).equals(host.essentia()))
                && (!(transport instanceof thaumcraft.api.aspects.IEssentiaStorage storage)
                    || storage.insert(host.essentia(),1,true)>0);
    }
    private static int tier(GolemWorkHost host,EssentiaJarBlockEntity jar) {
        if (!jar.accepts(host.essentia())) return -1;
        boolean matches=host.essentia().equals(jar.aspect()),empty=jar.aspect()==null || jar.amount()==0;
        if (jar.filter()!=null) {
            if (matches && jar.amount()>0 && jar.amount()<jar.capacity()) return 0;
            if (empty) return 1;
            if (matches && jar.amount()>=jar.capacity() && jar.voidJar()) return 2;
        } else {
            if (matches && jar.amount()>0 && jar.amount()<jar.capacity()) return 3;
            if (empty && !jar.voidJar()) return 4;
            if (matches && jar.voidJar()) return 5;
            if (empty && jar.voidJar()) return 6;
        }
        return -1;
    }
}
