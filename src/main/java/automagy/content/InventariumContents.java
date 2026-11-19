package automagy.content;

import automagy.api.InventariumProvider;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Rebuilds a bounded snapshot from loaded locations, never from clients or stale cached ItemStacks. */
public final class InventariumContents {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BLACKLIST=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.parse("automagy:inventarium_container_blacklist"));
    public record Source(BlockPos position,Direction side) { public Source {position=position.immutable();} }
    public record Entry(ItemStack item,long amount) {
        public Entry{item=item.copyWithCount(1);}
        @Override public ItemStack item(){return item.copy();}
    }
    public record Snapshot(List<Entry> entries,int slotsRead,boolean omitted){public Snapshot{entries=List.copyOf(entries);}}
    private record ItemKey(net.minecraft.world.item.Item item,net.minecraft.nbt.CompoundTag tag){
        ItemKey(ItemStack stack){this(stack.getItem(),stack.hasTag()?stack.getTag().copy():null);}
    }
    private record Registration(int priority,InventariumProvider provider) {}
    private static final Map<ResourceLocation,Registration> PROVIDERS=new LinkedHashMap<>();
    public static void register(ResourceLocation id,InventariumProvider provider){register(id,0,provider);}
    public static synchronized void register(ResourceLocation id,int priority,InventariumProvider provider){
        if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),new Registration(priority,Objects.requireNonNull(provider)))!=null)throw new IllegalArgumentException("Duplicate Inventarium provider: "+id);
    }
    public record Resolved(net.minecraftforge.items.IItemHandler handler,Object identity) {}
    private record ProviderIdentity(ResourceLocation provider,Object key) {}
    public static Optional<net.minecraftforge.items.IItemHandler> inventory(Level level,Source source){return resolve(level,source).map(Resolved::handler);}
    public static Optional<Resolved> resolve(Level level,Source source){
        if(!level.hasChunkAt(source.position))return Optional.empty();
        var state=level.getBlockState(source.position);
        if(state.is(BLACKLIST)||automagy.AutomagyConfig.matches(state,automagy.AutomagyConfig.INVENTORY_BLACKLIST))return Optional.empty();
        var tile=level.getBlockEntity(source.position);if(tile==null)return Optional.empty();
        Map<ResourceLocation,InventariumProvider> providers;
        synchronized(InventariumContents.class){providers=new LinkedHashMap<>();PROVIDERS.entrySet().stream().sorted(Comparator.comparingInt(e->e.getValue().priority())).forEach(e->providers.put(e.getKey(),e.getValue().provider()));}
        for(var entry:providers.entrySet()){
            var handler=entry.getValue().inventory(tile,source.side);
            if(handler.isPresent())return handler.filter(h->h.getSlots()>=0&&h.getSlots()<=65536)
                .map(h->new Resolved(h,new ProviderIdentity(entry.getKey(),Objects.requireNonNull(entry.getValue().identity(tile,source.side)))));
        }
        BlockPos identity=source.position;
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock&&state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
            var partner=source.position.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
            // Forge's double chest capability can query the adjacent tile. Guard before obtaining it.
            if(!level.hasChunkAt(partner))return Optional.empty();
            var other=level.getBlockState(partner);
            if(other.is(state.getBlock())&&other.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                &&other.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)
                &&other.getValue(net.minecraft.world.level.block.ChestBlock.FACING)==state.getValue(net.minecraft.world.level.block.ChestBlock.FACING)){
                if(partner.compareTo(identity)<0)identity=partner;
            }
        }
        var handler=InventoryTransfer.at(level,source.position,source.side);if(handler.isEmpty())return Optional.empty();
        // A single-half handler must retain its own identity; only merged views share a key.
        if(handler.get().getSlots()!=54)identity=source.position;
        return Optional.of(new Resolved(handler.get(),identity));
    }
    public static boolean sameInventory(Level level,Source a,Source b){
        if(a.position.equals(b.position))return true;
        var left=resolve(level,a);var right=resolve(level,b);
        return left.isPresent()&&right.isPresent()&&left.get().identity.equals(right.get().identity);
    }
    public static Snapshot read(Level level,List<Source> locations,int budget){return readResolved(locations,budget,source->resolve(level,source));}
    static Snapshot readResolved(List<Source> locations,int budget,java.util.function.Function<Source,Optional<Resolved>> resolver){
        int remaining=Math.max(0,budget),used=0;boolean omitted=false;Map<ItemKey,Entry> entries=new LinkedHashMap<>();
        Set<Object> visited=new HashSet<>();
        for(Source source:locations){
            var resolved=resolver.apply(source);
            if(resolved.isEmpty()||!visited.add(resolved.get().identity()))continue;
            var handler=Optional.of(resolved.get().handler());
            int slots=Math.max(0,handler.get().getSlots()),read=Math.min(slots,remaining);omitted|=slots>read;
            for(int slot=0;slot<read;slot++){
                ItemStack stack=handler.get().getStackInSlot(slot);if(stack.isEmpty())continue;
                var key=new ItemKey(stack);var previous=entries.get(key);
                entries.put(key,new Entry(stack,(previous==null?0:previous.amount)+stack.getCount()));
            }
            remaining-=read;used+=read;
        }
        return new Snapshot(new ArrayList<>(entries.values()),used,omitted);
    }
    private InventariumContents(){}
}
