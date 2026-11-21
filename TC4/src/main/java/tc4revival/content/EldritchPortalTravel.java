package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** One-to-one coordinate travel to an existing portal, without vanilla portal construction. */
public  class EldritchPortalTravel {
    private record Cached(BlockPos pos,long usedAt) {}
    private static final Map<ServerLevel,Map<Long,Cached>> CACHES=new WeakHashMap<>();
    private EldritchPortalTravel() {}
    public static Optional<BlockPos> find(ServerLevel destination,Vec3 from) {
        int x=net.minecraft.util.Mth.floor(from.x),z=net.minecraft.util.Mth.floor(from.z);
        long key=ChunkPos.asLong(x>>4,z>>4),now=destination.getGameTime();
        var cache=CACHES.computeIfAbsent(destination,unused->new HashMap<>());
        cache.values().removeIf(entry->entry.usedAt()<now-600);
        Cached previous=cache.get(key);
        if (previous!=null && destination.getBlockState(previous.pos()).is(TCEldritch.PORTAL.get())) {
            cache.put(key,new Cached(previous.pos(),now)); return Optional.of(previous.pos());
        }
        cache.remove(key);
        BlockPos best=null; double bestDistance=Double.POSITIVE_INFINITY;
        for (int cx=(x-128)>>4;cx<=(x+128)>>4;cx++) for (int cz=(z-128)>>4;cz<=(z+128)>>4;cz++) {
            // All portal blocks own a block entity; scanning that index avoids 25 million block reads.
            for (var entry:destination.getChunk(cx,cz).getBlockEntities().entrySet()) {
                BlockPos pos=entry.getKey();
                if (!(entry.getValue() instanceof EldritchPortalBlockEntity) || Math.abs((long)pos.getX()-x)>128
                        || Math.abs((long)pos.getZ()-z)>128 || !destination.getBlockState(pos).is(TCEldritch.PORTAL.get())) continue;
                double distance=Vec3.atCenterOf(pos).distanceToSqr(from);
                if (distance<bestDistance || distance==bestDistance && earlier(pos,best)) { best=pos; bestDistance=distance; }
            }
        }
        if (best!=null) cache.put(key,new Cached(best,now));
        return Optional.ofNullable(best);
    }
    private static boolean earlier(BlockPos a,BlockPos b) {
        return b==null || a.getX()<b.getX() || a.getX()==b.getX() && (a.getZ()<b.getZ() || a.getZ()==b.getZ() && a.getY()>b.getY());
    }
    public static boolean transfer(ServerPlayer player,ServerLevel destination) {
        var portal=find(destination,player.position()); if (portal.isEmpty()) return false;
        BlockPos pos=portal.get();
        Vec3 arrival=new Vec3(pos.getX()+.5+(destination.random.nextBoolean() ? 1 : -1),pos.getY(),
                pos.getZ()+.5+(destination.random.nextBoolean() ? 1 : -1));
        var info=new PortalInfo(arrival,Vec3.ZERO,player.getYRot(),player.getXRot());
        Entity moved=player.changeDimension(destination,new ITeleporter() {
            @Override public PortalInfo getPortalInfo(Entity entity,ServerLevel target,java.util.function.Function<ServerLevel,PortalInfo> vanilla) { return info; }
            @Override public Entity placeEntity(Entity entity,ServerLevel source,ServerLevel target,float yaw,java.util.function.Function<Boolean,Entity> reposition) {
                return reposition.apply(false);
            }
            @Override public boolean playTeleportSound(ServerPlayer entity,ServerLevel source,ServerLevel target) { return false; }
        });
        if (moved==null || player.serverLevel()!=destination) return false;
        player.setDeltaMovement(Vec3.ZERO); player.setPortalCooldown(100); return true;
    }
    public static void stopped(ServerStoppedEvent event) { CACHES.keySet().removeIf(level->level.getServer()==event.getServer()); }
}
