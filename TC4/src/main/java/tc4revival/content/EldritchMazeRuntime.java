package tc4revival.content;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Pure layout work may run in the background; all world and SavedData writes stay on the server. */
public  class EldritchMazeRuntime {
    private record Pending(int x,int z,CompletableFuture<EldritchMazeLayout> layout) {}
    private static  class State {
        final Map<Long,Pending> pending=new LinkedHashMap<>();
        final ArrayDeque<Long> rooms=new ArrayDeque<>();
        final Set<Long> queued=new HashSet<>();
        boolean restored;
    }
    private static final Map<MinecraftServer,State> SERVERS=new IdentityHashMap<>();
    private EldritchMazeRuntime() {}
    public static boolean schedule(ServerLevel source,int cx,int cz,int width,int height,long seed) {
        return schedule(source,cx,cz,width,height,seed,Math.floorMod((int)seed,4));
    }
    public static boolean schedule(ServerLevel source,int cx,int cz,int width,int height,long seed,int boss) {
        if(boss<0||boss>3)throw new IllegalArgumentException("Invalid maze boss");
        if (!canSchedule(source,cx,cz,width,height)) return false;
        var server=source.getServer();
        var state=SERVERS.computeIfAbsent(server,unused->new State());
        var reservation=new EldritchMazeSavedData.Reservation(cx,cz,width,height,seed);
        if (!EldritchMazeSavedData.get(server).reserve(reservation)) return false;
        EldritchMazeSavedData.get(server).assignBoss(cx,cz,boss);
        start(state,reservation);
        return true;
    }
    private static void start(State state,EldritchMazeSavedData.Reservation reservation) {
        state.pending.computeIfAbsent(ChunkPos.asLong(reservation.centerX(),reservation.centerZ()),unused->new Pending(
                reservation.centerX(),reservation.centerZ(),CompletableFuture.supplyAsync(()->EldritchMazeLayout.generate(
                        reservation.width(),reservation.height(),reservation.seed()))));
    }
    public static boolean canSchedule(ServerLevel source,int cx,int cz,int width,int height) {
        // Reject an impossible destination before the surface ring changes any terrain.
        try { new EldritchMazeSavedData.Reservation(cx,cz,width,height,0); }
        catch (IllegalArgumentException invalid) { return false; }
        var server=source.getServer(); var data=EldritchMazeSavedData.get(server);
        if (!data.readable() || server.getLevel(TCEldritch.OUTER_LANDS)==null || data.hasMazeInRange(cx,cz,width,height)) return false;
        for (var reservation:data.reservations())
            if (Math.abs((long)reservation.centerX()-cx)<=width && Math.abs((long)reservation.centerZ()-cz)<=height) return false;
        return true;
    }
    /** First calls schedule preparation; true requires a completed maze and a real portal within reach. */
    public static boolean request(ServerLevel source,BlockPos altar) {
        MinecraftServer server=source.getServer();
        ServerLevel outer=server.getLevel(TCEldritch.OUTER_LANDS); if (outer==null) return false;
        var data=EldritchMazeSavedData.get(server); var state=SERVERS.computeIfAbsent(server,unused->new State());
        if (!data.readable()) return false;
        int width=15+source.random.nextInt(8)*2,height=15+source.random.nextInt(8)*2;
        int cx=altar.getX()>>4,cz=altar.getZ()>>4;
        for (var plan:data.plans()) {
            BlockPos portal=new BlockPos(plan.centerX()*16+8,53,plan.centerZ()*16+8);
            if (Math.abs((long)portal.getX()-altar.getX())>128 || Math.abs((long)portal.getZ()-altar.getZ())>128) continue;
            enqueue(state,data,plan);
            if (complete(data,plan) && outer.getBlockState(portal).is(TCEldritch.PORTAL.get())) return true;
        }
        if (data.hasMazeInRange(cx,cz,width,height)) return false;
        for (var reservation:data.reservations())
            if (Math.abs((long)reservation.centerX()-cx)<=width && Math.abs((long)reservation.centerZ()-cz)<=height) {
                start(state,reservation); return false;
            }
        schedule(source,cx,cz,width,height,source.random.nextLong());
        return false;
    }
    private static void enqueue(State state,EldritchMazeSavedData data,EldritchMazeSavedData.Plan plan) {
        for (int z=0;z<plan.layout().height();z++) for (int x=0;x<plan.layout().width();x++) {
            if (plan.layout().cell(x,z).packed()==0) continue;
            int cx=plan.originX()+x,cz=plan.originZ()+z; long key=ChunkPos.asLong(cx,cz);
            if (!data.isGenerated(cx,cz) && state.queued.add(key)) state.rooms.addLast(key);
        }
    }
    private static boolean complete(EldritchMazeSavedData data,EldritchMazeSavedData.Plan plan) {
        for (int z=0;z<plan.layout().height();z++) for (int x=0;x<plan.layout().width();x++)
            if (plan.layout().cell(x,z).packed()!=0 && !data.isGenerated(plan.originX()+x,plan.originZ()+z)) return false;
        return true;
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        MinecraftServer server=event.getServer(); ServerLevel outer=server.getLevel(TCEldritch.OUTER_LANDS); if (outer==null) return;
        var state=SERVERS.computeIfAbsent(server,unused->new State()); var data=EldritchMazeSavedData.get(server);
        if (!state.restored) {
            for (var plan:data.plans()) enqueue(state,data,plan);
            for (var reservation:data.reservations()) start(state,reservation);
            state.restored=true;
        }
        for (var iterator=state.pending.values().iterator();iterator.hasNext();) {
            Pending pending=iterator.next(); if (!pending.layout().isDone()) continue;
            iterator.remove();
            try {
                var layout=pending.layout().join();
                if (!data.publish(pending.x(),pending.z(),layout)) throw new IllegalStateException("Maze plan overlaps existing rooms; reservation retained");
                enqueue(state,data,data.plan(pending.x(),pending.z()).orElseThrow());
            } catch (RuntimeException failure) { com.mojang.logging.LogUtils.getLogger().error("Eldritch maze planning failed at {},{}",pending.x(),pending.z(),failure); }
        }
        // Bound generation to one complete room per tick; never yield a partially placed room.
        if (!state.rooms.isEmpty()) {
            long key=state.rooms.removeFirst(); int cx=ChunkPos.getX(key),cz=ChunkPos.getZ(key);
            try {
                if (!data.isGenerated(cx,cz)) {
                    EldritchMazePlacer.generate(outer,cx,cz,data.cell(cx,cz).orElseThrow());
                    data.markGenerated(cx,cz);
                }
                state.queued.remove(key);
            } catch (RuntimeException failure) {
                // Keep it uncommitted and retry only after a new request or server restart.
                state.queued.remove(key);
                com.mojang.logging.LogUtils.getLogger().error("Eldritch room placement failed at {},{}; room remains uncommitted",cx,cz,failure);
            }
        }
    }
    public static void stopped(ServerStoppedEvent event) {
        State state=SERVERS.remove(event.getServer());
        if (state!=null) state.pending.values().forEach(pending->pending.layout().cancel(true));
    }
}
