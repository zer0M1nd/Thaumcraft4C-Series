package automagy.content;

import automagy.logic.InventariumStructure;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InventariumBlockEntity extends BlockEntity {
    private static final Map<Level,Set<BlockPos>> LOADED=new WeakHashMap<>();
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)LOADED.computeIfAbsent(level,k->new HashSet<>()).add(worldPosition);}
    @Override public void setRemoved(){if(level!=null){var positions=LOADED.get(level);if(positions!=null)positions.remove(worldPosition);}super.setRemoved();}
    public static Optional<InventariumBlockEntity> nearest(Level level,BlockPos pos){
        return nearest(level,pos,false);
    }
    public static Optional<InventariumBlockEntity> nearest(Level level,BlockPos pos,boolean memory){
        return LOADED.getOrDefault(level,Set.of()).stream().filter(level::hasChunkAt).map(level::getBlockEntity)
            .filter(InventariumBlockEntity.class::isInstance).map(InventariumBlockEntity.class::cast)
            .filter(core->core.capacity.loaded()&&core.inRange(pos)&&(memory?core.memoryUsers.size()<core.capacity.memories():core.nodes.size()<core.capacity.nodes()))
            .min(Comparator.comparingDouble(core->core.worldPosition.distSqr(pos)));
    }
    public boolean hasNode(BlockPos pos){return nodes.contains(pos);}
    public static boolean memoryInUse(Level level,BlockPos pos){
        return LOADED.getOrDefault(level,Set.of()).stream().filter(p->p.getY()-pos.getY()>=2&&p.getY()-pos.getY()<=3&&Math.abs(p.getX()-pos.getX())<=3&&Math.abs(p.getZ()-pos.getZ())<=3)
            .filter(level::hasChunkAt).map(level::getBlockEntity).anyMatch(tile->tile instanceof InventariumBlockEntity core&&core.capacity.loaded());
    }
    private final LinkedHashSet<BlockPos> nodes=new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> memoryUsers=new LinkedHashSet<>();
    private final InventariumRequestBook requests=new InventariumRequestBook(this::setChanged);
    public InventariumRequestBook requests(){return requests;}
    private InventariumStructure.Capacity capacity=new InventariumStructure.Capacity(0,1,10,0,false);
    private int validityCountdown;
    private boolean strike;
    private long snapshotTick=Long.MIN_VALUE;
    private InventariumContents.Snapshot snapshot=new InventariumContents.Snapshot(List.of(),0,false);
    public InventariumBlockEntity(BlockPos pos,BlockState state){super(Content.INVENTARIUM_TILE.get(),pos,state);}
    public InventariumStructure.Capacity capacity(){return capacity;}
    public boolean link(BlockPos pos){
        if(level==null||level.isClientSide||!capacity.loaded()||!level.hasChunkAt(pos)||!inRange(pos))return false;
        if(!(level.getBlockEntity(pos) instanceof InventariumNode node))return false;
        if(node.linkedCore()!=null&&!worldPosition.equals(node.linkedCore()))return false;
        if(!nodes.contains(pos)&&nodes.size()>=capacity.nodes())return false;
        nodes.add(pos.immutable());node.linkCore(worldPosition);invalidate();return true;
    }
    public void unlink(BlockPos pos){
        if(nodes.remove(pos)){detach(pos);invalidate();}
    }
    public boolean linkMemory(BlockPos pos){
        if(level==null||level.isClientSide||!capacity.loaded()||!level.hasChunkAt(pos)||!inRange(pos))return false;
        if(!(level.getBlockEntity(pos) instanceof InventariumMemoryUser user)||user.linkedCore()!=null&&!worldPosition.equals(user.linkedCore()))return false;
        if(!memoryUsers.contains(pos)&&memoryUsers.size()>=capacity.memories())return false;
        memoryUsers.add(pos.immutable());user.linkCore(worldPosition);setChanged();return true;
    }
    public boolean hasMemoryUser(BlockPos pos){return memoryUsers.contains(pos);}
    public boolean tracksDestinations(){
        return level!=null&&memoryUsers.stream().filter(level::hasChunkAt).map(level::getBlockEntity)
            .anyMatch(tile->tile instanceof DeliveryMirrorBlockEntity mirror&&mirror.multi()&&worldPosition.equals(mirror.linkedCore()));
    }
    public void unlinkMemory(BlockPos pos){if(memoryUsers.remove(pos)){detachMemory(pos);setChanged();}}
    private void detachMemory(BlockPos pos){
        if(level!=null&&!level.isClientSide)requests.cancelDestination(net.minecraft.core.GlobalPos.of(level.dimension(),pos));
        if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof InventariumMemoryUser user&&worldPosition.equals(user.linkedCore()))user.linkCore(null);
    }
    private void detach(BlockPos pos){
        if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof InventariumNode node&&worldPosition.equals(node.linkedCore()))node.linkCore(null);
    }
    public void breakLinks(){for(BlockPos pos:nodes)detach(pos);nodes.clear();for(BlockPos pos:memoryUsers)detachMemory(pos);memoryUsers.clear();invalidate();}
    private boolean inRange(BlockPos pos){
        return InventariumStructure.inRange(pos.getX()-worldPosition.getX(),pos.getY()-worldPosition.getY(),pos.getZ()-worldPosition.getZ(),capacity.distance());
    }
    private void invalidate(){snapshotTick=Long.MIN_VALUE;setChanged();}
    public Map<InventariumContents.Source,Integer> readableSources(){
        Map<InventariumContents.Source,Integer> result=new LinkedHashMap<>();
        if(level==null||level.isClientSide||!capacity.loaded())return result;
        Set<Object> visited=new HashSet<>();int remaining=capacity.slots();
        for(BlockPos pos:nodes)if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof InventariumNode node&&worldPosition.equals(node.linkedCore())){
            for(var source:node.sources()){
                var resolved=InventariumContents.resolve(level,source);if(resolved.isEmpty()||!visited.add(resolved.get().identity()))continue;
                var inventory=java.util.Optional.of(resolved.get().handler());
                int slots=Math.min(remaining,Math.max(0,inventory.get().getSlots()));if(slots>0)result.put(source,slots);remaining-=slots;if(remaining<=0)return result;
            }
        }return result;
    }
    public InventariumContents.Snapshot contents(){
        if(level==null||level.isClientSide||!capacity.loaded())return new InventariumContents.Snapshot(List.of(),0,false);
        if(snapshotTick!=level.getGameTime()){
            List<InventariumContents.Source> sources=new ArrayList<>();
            for(BlockPos pos:nodes)if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof InventariumNode node&&worldPosition.equals(node.linkedCore()))sources.addAll(node.sources());
            snapshot=InventariumContents.read(level,sources,capacity.slots());snapshotTick=level.getGameTime();
        }
        return snapshot;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,InventariumBlockEntity core){
        if(level.isClientSide||--core.validityCountdown>0)return;
        if(level instanceof net.minecraft.server.level.ServerLevel server)CourierCleanupData.get(server).apply(core);
        var environment=new InventariumEnvironment(level,pos);
        var validity=InventariumStructure.validate(environment,false);
        core.validityCountdown=80;
        if(validity==InventariumStructure.Validity.UNLOADED){core.capacity=new InventariumStructure.Capacity(0,1,10,0,false);return;}
        if(validity==InventariumStructure.Validity.INVALID){
            core.capacity=new InventariumStructure.Capacity(0,1,10,0,false);
            if(core.strike)level.destroyBlock(pos,true);else{core.strike=true;core.validityCountdown=40;core.setChanged();}
            return;
        }
        core.strike=false;var updated=InventariumStructure.capacity(environment);core.capacity=updated;
        if(!updated.loaded())return;
        int kept=0;
        for(var iterator=core.nodes.iterator();iterator.hasNext();){
            BlockPos nodePos=iterator.next();
            boolean missing=level.hasChunkAt(nodePos)&&(!(level.getBlockEntity(nodePos) instanceof InventariumNode node)||!pos.equals(node.linkedCore()));
            if(missing||!core.inRange(nodePos)||kept>=updated.nodes()){iterator.remove();core.detach(nodePos);}else kept++;
        }
        kept=0;
        for(var iterator=core.memoryUsers.iterator();iterator.hasNext();){
            BlockPos userPos=iterator.next();
            boolean missing=level.hasChunkAt(userPos)&&(!(level.getBlockEntity(userPos) instanceof InventariumMemoryUser user)||!pos.equals(user.linkedCore()));
            if(missing||!core.inRange(userPos)||kept>=updated.memories()){iterator.remove();core.detachMemory(userPos);}else kept++;
        }
        core.invalidate();
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ListTag saved=new ListTag();for(BlockPos pos:nodes)saved.add(NbtUtils.writeBlockPos(pos));tag.put("Nodes",saved);tag.putBoolean("StructureStrike",strike);
        ListTag memory=new ListTag();for(BlockPos pos:memoryUsers)memory.add(NbtUtils.writeBlockPos(pos));tag.put("MemoryUsers",memory);
        tag.put("RequestBook",requests.save());
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);nodes.clear();var saved=tag.getList("Nodes",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(344,saved.size());i++)nodes.add(NbtUtils.readBlockPos(saved.getCompound(i)));
        memoryUsers.clear();var memory=tag.getList("MemoryUsers",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(98,memory.size());i++)memoryUsers.add(NbtUtils.readBlockPos(memory.getCompound(i)));
        strike=tag.getBoolean("StructureStrike");validityCountdown=0;snapshotTick=Long.MIN_VALUE;
        requests.load(tag.getCompound("RequestBook"));
    }
}
