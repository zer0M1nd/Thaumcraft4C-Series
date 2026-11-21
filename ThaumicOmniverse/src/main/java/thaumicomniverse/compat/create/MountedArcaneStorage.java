package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaSource;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.EssentiaStorageApi;

/** Owns detached snapshots. Their level remains null: jar callbacks must never edit a real local coordinate. */
public class MountedArcaneStorage implements IEssentiaStorage {
    private final Contraption contraption;
    private final Map<BlockPos,Entry> entries = new LinkedHashMap<>();
    private final Set<BlockPos> inspected = new HashSet<>();
    public MountedArcaneStorage(Contraption contraption){this.contraption=contraption;}
    public static MountedArcaneStorage of(Contraption contraption){return ((ContraptionAccess)contraption).omniverse$storage();}
    public boolean live(){return !contraption.disassembled&&contraption.entity!=null&&!contraption.entity.isRemoved();}
    public Collection<Entry> entries(){
        if(!live())return List.of();
        for(var info:contraption.getBlocks().values()) {
            if(info.nbt()==null||!inspected.add(info.pos()))continue;
            var entity=BlockEntity.loadStatic(info.pos(),info.state(),info.nbt().copy());
            if(EssentiaStorageApi.supports(entity,null))entries.put(info.pos(),new Entry(info.pos(),entity));
        }
        return Collections.unmodifiableCollection(entries.values());
    }
    public Entry at(BlockPos local){entries();return live()?entries.get(local):null;}
    @Override public AspectAmounts contents(){
        var result=new AspectAmounts();for(var entry:entries())for(var aspect:entry.storage.contents().aspects()) {
            long sum=(long)result.amount(aspect)+entry.storage.contents().amount(aspect);
            result.remove(aspect).add(aspect,(int)Math.min(Integer.MAX_VALUE,sum));
        }return result;
    }
    @Override public int capacity(Aspect a){
        long sum=0;for(var entry:entries())sum=Math.min(Integer.MAX_VALUE,sum+entry.storage.capacity(a));return (int)sum;
    }
    @Override public int totalCapacity(){
        long sum=0;for(var entry:entries())sum=Math.min(Integer.MAX_VALUE,sum+entry.storage.totalCapacity());return (int)sum;
    }
    @Override public int insert(Aspect a,int amount,boolean simulate){return move(a,amount,simulate,true);}
    @Override public int extract(Aspect a,int amount,boolean simulate){return move(a,amount,simulate,false);}
    private int move(Aspect a,int amount,boolean simulate,boolean insert){
        Objects.requireNonNull(a);if(amount<0)throw new IllegalArgumentException("Negative essentia");
        int remaining=amount;
        for(var entry:entries()){
            remaining-=insert?entry.storage.insert(a,remaining,simulate):entry.storage.extract(a,remaining,simulate);
            if(remaining==0)break;
        }return amount-remaining;
    }
    public class Entry implements EssentiaSource {
        private final BlockPos local;
        private final BlockEntity entity;
        public final IEssentiaStorage storage;
        Entry(BlockPos local,BlockEntity entity){
            this.local=local.immutable();this.entity=entity;
            storage=EssentiaStorageApi.detached(this::current,this::save);
        }
        private BlockEntity current(){
            if(!live())return null;
            var info=contraption.getBlocks().get(local);
            if(info==null||info.nbt()==null)return null;
            // Movement actors may have changed the same vessel since the last transfer.
            // Refresh before resolving an operation, then persist only after it succeeds.
            entity.load(info.nbt().copy());
            return entity;
        }
        public BlockPos local(){return local;}
        public BlockEntity entity(){return current();}
        public Vec3 position(){return contraption.entity.toGlobalVector(local.getCenter().add(0,.375,0),1);}
        public void save(){
            var info=contraption.getBlocks().get(local);if(info==null||info.nbt()==null||!live())return;
            var saved=entity.saveWithFullMetadata();
            // Preserve this tag's identity: Create actors retain references to the captured NBT.
            for(var key:new HashSet<>(info.nbt().getAllKeys()))info.nbt().remove(key);
            info.nbt().merge(saved);
            var update=entity.getUpdateTag();((ContraptionAccess)contraption).omniverse$updates().put(local,update);
            CreateArcaneNetwork.sync(contraption.entity,local,update);
        }
        @Override public boolean contains(Aspect a,int n){return n>=0&&storage.extract(a,n,true)==n;}
        @Override public boolean takeFromContainer(Aspect a,int n){return contains(a,n)&&storage.extract(a,n,false)==n;}
        @Override public int availableAmount(Aspect a){return storage.contents().amount(a);}
    }
}
