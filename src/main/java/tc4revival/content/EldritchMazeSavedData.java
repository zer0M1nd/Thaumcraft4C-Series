package tc4revival.content;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned maze plans. Publish whole layouts before permitting room generation. */
public  class EldritchMazeSavedData extends SavedData {
    private static final java.util.concurrent.ConcurrentMap<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,java.util.function.Function<net.minecraft.server.level.ServerLevel,EldritchMazeSavedData>> DIMENSION_PROVIDERS=new java.util.concurrent.ConcurrentHashMap<>();
    /** Startup registration only. Providers must obtain world-owned SavedData, not retain a ServerLevel. */
    public static void registerDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,java.util.function.Function<net.minecraft.server.level.ServerLevel,EldritchMazeSavedData> provider) {
        java.util.Objects.requireNonNull(dimension);java.util.Objects.requireNonNull(provider);
        if (dimension.equals(TCEldritch.OUTER_LANDS)) throw new IllegalArgumentException("Cannot replace the Thaumcraft outer lands");
        if (DIMENSION_PROVIDERS.putIfAbsent(dimension,provider)!=null) throw new IllegalArgumentException("Maze dimension already registered: "+dimension.location());
    }
    public static boolean isMazeDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        return dimension.equals(TCEldritch.OUTER_LANDS)||DIMENSION_PROVIDERS.containsKey(dimension);
    }
    public static EldritchMazeSavedData get(net.minecraft.server.level.ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Maze access requires the server thread");
        var provider=DIMENSION_PROVIDERS.get(level.dimension());
        return provider==null?get(level.getServer()):java.util.Objects.requireNonNull(provider.apply(level),"Maze provider returned no data");
    }
    private static final int VERSION=1;
    private final Map<Long,EldritchMazeLayout.Cell> cells=new LinkedHashMap<>();
    private final Set<Long> generated=new HashSet<>();
    private final Map<Long,Plan> plans=new LinkedHashMap<>();
    private final Map<Long,Reservation> reservations=new LinkedHashMap<>();
    private CompoundTag unreadable;
    private int bossCount;
    private final Map<Long,Integer> bosses=new LinkedHashMap<>();
    public void assignBoss(int cx,int cz,int boss) {
        writable();if(boss<0||boss>3)throw new IllegalArgumentException("Invalid boss");
        long key=ChunkPos.asLong(cx,cz);
        if(!plans.containsKey(key)&&!reservations.containsKey(key))throw new IllegalArgumentException("No maze reservation");
        var previous=bosses.putIfAbsent(key,boss);
        if(previous!=null&&previous!=boss)throw new IllegalStateException("Maze boss already selected");setDirty();
    }
    public int bossAt(int roomX,int roomZ) {
        for(var entry:plans.entrySet()) {
            var plan=entry.getValue();int x=roomX-plan.originX(),z=roomZ-plan.originZ();
            if(x>=0&&z>=0&&x<plan.layout().width()&&z<plan.layout().height()&&plan.layout().cell(x,z).packed()!=0)
                return bosses.getOrDefault(entry.getKey(),Math.floorMod((int)plan.layout().seed(),4));
        }
        throw new IllegalArgumentException("Room has no maze plan");
    }
    public boolean readable() { return unreadable==null; }

    public int nextBoss(net.minecraft.util.RandomSource random) {
        writable(); bossCount++;
        if (random.nextFloat()<.25f) bossCount++;
        setDirty(); return Math.floorMod(bossCount,4);
    }

    public record Plan(int centerX,int centerZ,EldritchMazeLayout layout) {
        public int originX() { return centerX-layout.entrance().x(); }
        public int originZ() { return centerZ-layout.entrance().z(); }
    }
    public record Reservation(int centerX,int centerZ,int width,int height,long seed) {
        public Reservation {
            if (width<7 || height<7 || width>63 || height>63) throw new IllegalArgumentException("Invalid reserved maze dimensions");
            long x=(long)centerX-(1+width/2),z=(long)centerZ-(1+height/2);
            if (x< -1874999 || z< -1874999 || x+width>1875000 || z+height>1875000)
                throw new IllegalArgumentException("Reserved maze outside world bounds");
        }
    }
    public Collection<Reservation> reservations() { return List.copyOf(reservations.values()); }
    public boolean reserve(Reservation reservation) {
        writable(); long key=ChunkPos.asLong(reservation.centerX(),reservation.centerZ());
        if (plans.containsKey(key) || reservations.containsKey(key)) return false;
        reservations.put(key,reservation); setDirty(); return true;
    }

    public static EldritchMazeSavedData get(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Maze access requires the server thread");
        return server.overworld().getDataStorage().computeIfAbsent(EldritchMazeSavedData::load,
                EldritchMazeSavedData::new,"thaumcraft_labyrinth");
    }

    public Optional<EldritchMazeLayout.Cell> cell(int x,int z) { return Optional.ofNullable(cells.get(ChunkPos.asLong(x,z))); }
    public Optional<Plan> plan(int centerX,int centerZ) { return Optional.ofNullable(plans.get(ChunkPos.asLong(centerX,centerZ))); }
    public Collection<Plan> plans() { return List.copyOf(plans.values()); }
    public boolean isGenerated(int x,int z) { return generated.contains(ChunkPos.asLong(x,z)); }

    /** Legacy exclusion rectangle is measured in chunks, with inclusive edges. */
    public boolean hasMazeInRange(int x,int z,int width,int height) {
        if (width<0 || height<0) throw new IllegalArgumentException("Negative exclusion range");
        for (long key:cells.keySet()) {
            if (Math.abs((long)ChunkPos.getX(key)-x)<=width && Math.abs((long)ChunkPos.getZ(key)-z)<=height) return true;
        }
        return false;
    }

    /** No partial writes on overlap or invalid placement; generation itself may run off-thread. */
    public boolean publish(int centerX,int centerZ,EldritchMazeLayout layout) {
        writable(); Objects.requireNonNull(layout);
        long center=ChunkPos.asLong(centerX,centerZ);
        if (plans.containsKey(center)) return false;
        var plan=new Plan(centerX,centerZ,layout);
        long minX=(long)centerX-layout.entrance().x(),minZ=(long)centerZ-layout.entrance().z();
        // Stay inside Minecraft's block-coordinate boundary, including each complete room.
        if (minX< -1874999 || minZ< -1874999 || minX+layout.width()>1875000 || minZ+layout.height()>1875000)
            throw new IllegalArgumentException("Maze extends outside world bounds");
        var additions=new LinkedHashMap<Long,EldritchMazeLayout.Cell>();
        for (int z=0;z<layout.height();z++) for (int x=0;x<layout.width();x++) {
            var cell=layout.cell(x,z); if (cell.packed()==0) continue;
            long key=ChunkPos.asLong(plan.originX()+x,plan.originZ()+z);
            if (cells.containsKey(key)) return false;
            additions.put(key,cell);
        }
        if (layout.cell(layout.entrance().x(),layout.entrance().z()).feature()!=1)
            throw new IllegalArgumentException("Maze has no entrance at its center");
        cells.putAll(additions); plans.put(center,plan); reservations.remove(center); setDirty(); return true;
    }

    /** Called only after the room's blocks and block entities have been committed. */
    public void markGenerated(int x,int z) {
        writable(); long key=ChunkPos.asLong(x,z);
        if (!cells.containsKey(key)) throw new IllegalArgumentException("Cannot generate an unplanned room");
        if (generated.add(key)) setDirty();
    }

    private void writable() {
        if (unreadable!=null) throw new IllegalStateException("Unreadable labyrinth data retained; refusing to overwrite it");
    }

    public static EldritchMazeSavedData load(CompoundTag tag) {
        var data=new EldritchMazeSavedData();
        try {
            if (tag.getInt("Version")!=VERSION || !tag.contains("Plans",Tag.TAG_LIST) || !tag.contains("Generated",Tag.TAG_LONG_ARRAY))
                throw new IllegalArgumentException("Unsupported labyrinth format");
            if (tag.get("Plans") instanceof ListTag list && !list.isEmpty() && list.getElementType()!=Tag.TAG_COMPOUND)
                throw new IllegalArgumentException("Malformed maze plans");
            ListTag entries=tag.getList("Plans",Tag.TAG_COMPOUND);
            for (int i=0;i<entries.size();i++) {
                CompoundTag entry=entries.getCompound(i);
                if (!entry.contains("Center",Tag.TAG_LONG) || !entry.contains("Seed",Tag.TAG_LONG)
                        || !entry.contains("Width",Tag.TAG_INT) || !entry.contains("Height",Tag.TAG_INT)
                        || !entry.contains("Cells",Tag.TAG_INT_ARRAY)) throw new IllegalArgumentException("Incomplete maze plan");
                long center=entry.getLong("Center");
                var layout=EldritchMazeLayout.restore(entry.getInt("Width"),entry.getInt("Height"),entry.getLong("Seed"),entry.getIntArray("Cells"));
                if (!data.publish(ChunkPos.getX(center),ChunkPos.getZ(center),layout)) throw new IllegalArgumentException("Overlapping maze plans");
            }
            for (long key:tag.getLongArray("Generated")) {
                if (!data.cells.containsKey(key)) throw new IllegalArgumentException("Generated room has no plan");
                data.generated.add(key);
            }
            if (tag.contains("Reservations") && (!(tag.get("Reservations") instanceof ListTag list)
                    || !list.isEmpty() && list.getElementType()!=Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Malformed maze reservations");
            ListTag reserved=tag.getList("Reservations",Tag.TAG_COMPOUND);
            for (int i=0;i<reserved.size();i++) {
                CompoundTag entry=reserved.getCompound(i);
                if (!entry.contains("Center",Tag.TAG_LONG) || !entry.contains("Seed",Tag.TAG_LONG)
                        || !entry.contains("Width",Tag.TAG_INT) || !entry.contains("Height",Tag.TAG_INT))
                    throw new IllegalArgumentException("Incomplete maze reservation");
                long center=entry.getLong("Center");
                if (!data.reserve(new Reservation(ChunkPos.getX(center),ChunkPos.getZ(center),entry.getInt("Width"),entry.getInt("Height"),entry.getLong("Seed"))))
                    throw new IllegalArgumentException("Duplicate maze reservation");
            }
            var selected=tag.getList("Bosses",Tag.TAG_COMPOUND);
            for(int i=0;i<selected.size();i++) {var row=selected.getCompound(i);long center=row.getLong("Center");data.assignBoss(ChunkPos.getX(center),ChunkPos.getZ(center),row.getInt("Kind"));}
            data.setDirty(false);
            data.bossCount=tag.getInt("BossCount");
        } catch (RuntimeException failure) {
            data.cells.clear(); data.plans.clear(); data.generated.clear(); data.reservations.clear(); data.unreadable=tag.copy(); data.setDirty(false);
            com.mojang.logging.LogUtils.getLogger().error("Unable to read eldritch labyrinth; original data preserved",failure);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        if (unreadable!=null) return unreadable.copy();
        tag.putInt("Version",VERSION); tag.putInt("BossCount",bossCount); var entries=new ListTag();
        for (var entry:plans.entrySet()) {
            var plan=entry.getValue(); var layout=plan.layout(); var encoded=new CompoundTag();
            encoded.putLong("Center",entry.getKey()); encoded.putLong("Seed",layout.seed());
            encoded.putInt("Width",layout.width()); encoded.putInt("Height",layout.height());
            short[] packed=layout.packedCells(); int[] values=new int[packed.length];
            for (int i=0;i<packed.length;i++) values[i]=packed[i]&65535;
            encoded.putIntArray("Cells",values); entries.add(encoded);
        }
        tag.put("Plans",entries); tag.putLongArray("Generated",generated.stream().mapToLong(Long::longValue).sorted().toArray());
        var reserved=new ListTag();
        for (var reservation:reservations.values()) {
            var encoded=new CompoundTag(); encoded.putLong("Center",ChunkPos.asLong(reservation.centerX(),reservation.centerZ()));
            encoded.putInt("Width",reservation.width()); encoded.putInt("Height",reservation.height()); encoded.putLong("Seed",reservation.seed());
            reserved.add(encoded);
        }
        tag.put("Reservations",reserved);
        var selected=new ListTag();bosses.forEach((center,kind)->{var row=new CompoundTag();row.putLong("Center",center);row.putInt("Kind",kind);selected.add(row);});tag.put("Bosses",selected);
        return tag;
    }
}
