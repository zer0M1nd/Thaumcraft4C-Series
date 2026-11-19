package thaumcraft.api.aspects;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;

/** Stable side-aware handles re-resolve the block entity every call; never force chunk loads. */
public  class EssentiaStorageApi {
    @FunctionalInterface public interface Provider { IEssentiaStorage find(BlockEntity entity,Direction side); }
    private static final SortedMap<ResourceLocation,Provider> PROVIDERS=new TreeMap<>();
    public static synchronized void register(ResourceLocation id,Provider provider){if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate essentia provider "+id);}
    public static  class Changed extends net.minecraftforge.eventbus.api.Event {
        private final BlockEntity entity;
        public Changed(BlockEntity entity){this.entity=entity;}
        public BlockEntity entity(){return entity;}
    }
    /** Owners call this for changes made outside an API transfer; listeners must re-query instead of caching mutable inventories. */
    public static void changed(BlockEntity entity){if(entity.getLevel()!=null&&!entity.getLevel().isClientSide)net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new Changed(entity));}
    public static IEssentiaStorage at(ServerLevel level,BlockPos position,Direction side){return new Handle(level,position.immutable(),Objects.requireNonNull(side));}
    /** Inventory automation may access a vessel's contents independently of its pipe connection faces. */
    public static IEssentiaStorage atInventory(ServerLevel level,BlockPos position){return new Handle(level,position.immutable(),null);}
    public static IEssentiaStorage empty(){return EMPTY;}
    /** Non-mutating capability probe; transport-only pipes are deliberately excluded. */
    public static boolean supports(BlockEntity entity,Direction side){
        if(entity==null||entity.isRemoved())return false;
        for(var provider:PROVIDERS.values())if(provider.find(entity,side)!=null)return true;
        return entity instanceof IEssentiaStorage||entity instanceof EssentiaVessel||entity instanceof IAspectContainer||entity instanceof EssentiaReservoirBlockEntity;
    }
    private static final IEssentiaStorage EMPTY=new IEssentiaStorage(){public AspectAmounts contents(){return new AspectAmounts();}public int capacity(Aspect a){return 0;}public int insert(Aspect a,int n,boolean s){return 0;}public int extract(Aspect a,int n,boolean s){return 0;}};
    /** A detached owner (for example a moving structure) must persist/synchronize after successful writes.
     * The supplier is resolved for every call so replaced or dismantled inventories cannot be mutated.
     */
    public static IEssentiaStorage detached(java.util.function.Supplier<BlockEntity> owner,Runnable changed) {
        Objects.requireNonNull(owner); Objects.requireNonNull(changed);
        return new IEssentiaStorage() {
            public AspectAmounts contents(){return resolve(owner.get(),null).contents().copy();}
            public int totalCapacity(){return physicalCapacity(owner.get(),null);}
            public int capacity(Aspect a){return Math.max(0,resolve(owner.get(),null).capacity(Objects.requireNonNull(a)));}
            public int insert(Aspect a,int n,boolean simulate){return move(a,n,simulate,true);}
            public int extract(Aspect a,int n,boolean simulate){return move(a,n,simulate,false);}
            private int move(Aspect a,int n,boolean simulate,boolean insert){
                Objects.requireNonNull(a);if(n<0)throw new IllegalArgumentException("Negative essentia");if(n==0)return 0;
                var storage=resolve(owner.get(),null);
                int moved=insert?storage.insert(a,n,simulate):storage.extract(a,n,simulate);
                if(moved<0||moved>n)throw new IllegalStateException("Invalid storage transfer result");
                if(moved>0&&!simulate)changed.run();return moved;
            }
        };
    }
    /** Transfer adapters may predate totalCapacity and estimate only per-aspect free
     * space. A native vessel still owns its physical size, independently of that
     * adapter and of the selected pipe face. Preserve adapter transfer semantics. */
    private static int physicalCapacity(BlockEntity entity,Direction side){
        if(entity==null||entity.isRemoved())return 0;
        if(entity instanceof EssentiaReservoirBlockEntity)return EssentiaReservoirBlockEntity.CAPACITY;
        if(entity instanceof EssentiaVessel vessel)return Math.max(0,vessel.capacity());
        return Math.max(0,resolve(entity,side).totalCapacity());
    }
    private static IEssentiaStorage resolve(BlockEntity entity,Direction side){
            if(entity==null||entity.isRemoved())return EMPTY;
            for(var provider:PROVIDERS.values()){var result=provider.find(entity,side==null?Direction.UP:side);if(result!=null)return result;}
            if(entity instanceof IEssentiaStorage storage)return storage;
            if(entity instanceof EssentiaReservoirBlockEntity reservoir)return new IEssentiaStorage(){
                public int totalCapacity(){return EssentiaReservoirBlockEntity.CAPACITY;}
                public AspectAmounts contents(){return output(entity,side)?reservoir.revealedAspects():new AspectAmounts();}
                public int capacity(Aspect a){return input(entity,side)?reservoir.revealedAspects().amount(a)+Math.max(0,EssentiaReservoirBlockEntity.CAPACITY-reservoir.amount()):0;}
                public int insert(Aspect a,int n,boolean simulate){if(!input(entity,side))return 0;int accepted=Math.min(n,Math.max(0,EssentiaReservoirBlockEntity.CAPACITY-reservoir.amount()));return simulate?accepted:accepted-reservoir.addToContainer(a,accepted);}
                public int extract(Aspect a,int n,boolean simulate){if(!output(entity,side))return 0;int taken=Math.min(n,reservoir.revealedAspects().amount(a));return simulate?taken:reservoir.takeFromContainer(a,taken)?taken:0;}
            };
            if(side==null&&entity instanceof AdvancedAlchemyBlockEntity furnace){
                var core=furnace.controller();if(core==null)return EMPTY;
                return new IEssentiaStorage(){
                    public AspectAmounts contents(){return core.process().contents();}
                    public int capacity(Aspect a){return 0;}
                    public int insert(Aspect a,int n,boolean simulate){return 0;}
                    public int extract(Aspect a,int n,boolean simulate){int taken=Math.min(n,core.process().amount(a));return simulate?taken:core.process().take(a,taken,core)?taken:0;}
                };
            }
            if(entity instanceof EssentiaVessel vessel)return vessel(entity,vessel,side);
            if(entity instanceof IAspectContainer container)return container(entity,container,side);
            return EMPTY; // A transport-only pipe has no reliable simulated capacity. Register an explicit adapter.
    }
    private record Handle(ServerLevel level,BlockPos position,Direction side) implements IEssentiaStorage {
        private BlockEntity entity(){if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread only");return level.hasChunkAt(position)?level.getBlockEntity(position):null;}
        private IEssentiaStorage resolve(BlockEntity entity){return EssentiaStorageApi.resolve(entity,side);}
        public AspectAmounts contents(){return resolve(entity()).contents().copy();}
        public int totalCapacity(){return physicalCapacity(entity(),side);}
        public int capacity(Aspect a){Objects.requireNonNull(a);return Math.max(0,resolve(entity()).capacity(a));}
        public int insert(Aspect a,int n,boolean simulate){return move(a,n,simulate,true);}
        public int extract(Aspect a,int n,boolean simulate){return move(a,n,simulate,false);}
        private int move(Aspect a,int n,boolean simulate,boolean insert){Objects.requireNonNull(a);if(n<0)throw new IllegalArgumentException("Negative essentia");if(n==0)return 0;var owner=entity();var storage=resolve(owner);int result=insert?storage.insert(a,n,simulate):storage.extract(a,n,simulate);if(result<0||result>n)throw new IllegalStateException("Invalid storage transfer result");if(result>0&&!simulate&&owner!=null){owner.setChanged();changed(owner);}return result;}
    }
    private static boolean input(BlockEntity entity,Direction side){return side==null||!(entity instanceof EssentiaTransport t)||t.connectable(side)&&t.canInput(side);}
    private static boolean output(BlockEntity entity,Direction side){return side==null||!(entity instanceof EssentiaTransport t)||t.connectable(side)&&t.canOutput(side);}
    private static IEssentiaStorage vessel(BlockEntity owner,EssentiaVessel vessel,Direction side){return new IEssentiaStorage(){
        public int totalCapacity(){return Math.max(0,vessel.capacity());}
        public AspectAmounts contents(){return output(owner,side)&&vessel.aspect()!=null?new AspectAmounts().add(vessel.aspect(),Math.max(0,vessel.amount())):new AspectAmounts();}
        public int capacity(Aspect a){return input(owner,side)&&vessel.accepts(a)?Math.max(0,vessel.capacity()):0;}
        public int insert(Aspect a,int n,boolean simulate){if(!input(owner,side)||!vessel.accepts(a)||vessel.amount()>0&&!a.equals(vessel.aspect()))return 0;int accepted=Math.min(n,Math.max(0,vessel.capacity()-vessel.amount()));return simulate?accepted:accepted-vessel.addToContainer(a,accepted);}
        public int extract(Aspect a,int n,boolean simulate){if(!output(owner,side)||!a.equals(vessel.aspect()))return 0;int taken=Math.min(n,Math.max(0,vessel.amount()));return simulate?taken:vessel.takeFromContainer(a,taken)?taken:0;}
    };}
    private static IEssentiaStorage container(BlockEntity owner,IAspectContainer container,Direction side){return new IEssentiaStorage(){
        public AspectAmounts contents(){return output(owner,side)?container.getAspects().unwrap():new AspectAmounts();}
        public int capacity(Aspect a){return input(owner,side)&&container.accepts(a)?Math.max(0,container.capacity(a)):0;}
        public int insert(Aspect a,int n,boolean simulate){if(!input(owner,side)||!container.accepts(a))return 0;int accepted=Math.min(n,Math.max(0,container.capacity(a)-container.getAspects().unwrap().amount(a)));return simulate?accepted:accepted-container.addToContainer(a,accepted);}
        public int extract(Aspect a,int n,boolean simulate){if(!output(owner,side))return 0;int taken=Math.min(n,Math.max(0,container.getAspects().unwrap().amount(a)));return simulate?taken:container.takeFromContainer(a,taken)?taken:0;}
    };}
    public record Transfer(int inserted,int returnedToSource,int remainder){}
    /** Preflight is not a reservation. Rejected destination remainder is restored when possible.
     * The caller MUST retain result.remainder() in its own buffer if the source refuses restoration.
     * No world-agnostic adapter can promise atomicity for arbitrary third-party storage callbacks.
     */
    public static Transfer transfer(IEssentiaStorage source,IEssentiaStorage destination,Aspect aspect,int amount){
        if(amount<0)throw new IllegalArgumentException("Negative transfer");
        if(source==destination||source.equals(destination))return new Transfer(0,0,0);
        int planned=Math.min(source.extract(aspect,amount,true),destination.insert(aspect,amount,true));
        int taken=source.extract(aspect,planned,false);int inserted=destination.insert(aspect,taken,false);int rejected=taken-inserted;int restored=rejected==0?0:source.insert(aspect,rejected,false);
        return new Transfer(inserted,restored,rejected-restored);
    }
    private EssentiaStorageApi(){}
}
