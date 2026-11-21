package thaumcraft.api.devices;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import thaumcraft.api.util.ExtensionRegistry;

/** Typed, host-owned device extensions. Unknown payloads survive saves even when their addon is absent. */
public final class DeviceExtensions {
    public enum Removal { DESTROYED, REPLACED, MOVED, UNLOADED }
    public enum InventoryChange { REPLACED, EXTRACTED, CLEARED, PROCESS_RESULT }
    public interface Host { Store extensions(); }
    public interface Component {
        default void extensionAttached() {}
        default void extensionTick() {}
        default CompoundTag extensionSave(){return new CompoundTag();}
        default void extensionLoad(CompoundTag data) {}
        default void extensionInventoryChanged(InventoryChange change) {}
        default void extensionRemoved(Removal reason) {}
        default void extensionInvalidate() {}
        default void extensionRevive() {}
        default <T> LazyOptional<T> extensionCapability(Capability<T> capability,Direction side){return LazyOptional.empty();}
    }
    public record Key<C extends Component>(String id,Class<C> type){
        public Key {if(id==null||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid component id");Objects.requireNonNull(type);}
    }
    private record Registration(Key<?> key,Class<? extends BlockEntity> host,int version,String legacy,Function<BlockEntity,? extends Component> create) {}
    private static final ExtensionRegistry<Registration> REGISTRY=new ExtensionRegistry<>();
    private DeviceExtensions() {}
    public static <H extends BlockEntity,C extends Component> void register(Key<C> key,int priority,Class<H> host,int version,String legacy,Function<H,C> factory){
        if(version<1)throw new IllegalArgumentException("Component version must be positive");
        REGISTRY.register(key.id(),priority,new Registration(key,host,version,legacy==null?"":legacy,value->factory.apply(host.cast(value))));
    }
    public static <C extends Component> Optional<C> component(BlockEntity host,Key<C> key){
        return host instanceof Host owner&&!host.isRemoved()?owner.extensions().get(key):Optional.empty();
    }
    public static final class Store {
        private final BlockEntity host;
        private final Set<String> nativeFields;
        private final Map<String,Component> components=new LinkedHashMap<>();
        private CompoundTag pending=new CompoundTag(),legacy=new CompoundTag();
        private boolean capsValid=true,removed;
        public Store(BlockEntity host,String... nativeFields){this.host=host;this.nativeFields=new HashSet<>(List.of(nativeFields));}
        private void ensure(){
            if(host.getLevel()==null||host.isRemoved()||removed)return;
            for(var entry:REGISTRY.entries()){
                var registration=entry.value();var id=entry.id();
                if(!registration.host().isInstance(host)||components.containsKey(id))continue;
                var saved=pending.getCompound(id);
                if(pending.contains(id)&&saved.getInt("Version")!=registration.version())continue;
                var component=Objects.requireNonNull(registration.create().apply(host));
                try{
                    component.extensionLoad(pending.contains(id)?saved.getCompound("Data").copy():legacy.getCompound(registration.legacy()).copy());
                    component.extensionAttached();if(!capsValid)component.extensionInvalidate();components.put(id,component);
                }catch(RuntimeException error){component.extensionInvalidate();throw error;}
            }
        }
        public <C extends Component> Optional<C> get(Key<C> key){if(removed||host.isRemoved())return Optional.empty();ensure();var value=components.get(key.id());return key.type().isInstance(value)?Optional.of(key.type().cast(value)):Optional.empty();}
        public void tick(){ensure();if(host.getLevel()!=null&&!host.getLevel().isClientSide&&!removed)for(var value:List.copyOf(components.values()))value.extensionTick();}
        public void inventoryChanged(InventoryChange change){ensure();for(var value:List.copyOf(components.values()))value.extensionInventoryChanged(change);}
        public void load(CompoundTag root){
            pending=root.getCompound("DeviceExtensions").copy();legacy=new CompoundTag();
            for(String key:root.getAllKeys())if(root.get(key) instanceof CompoundTag&&!nativeFields.contains(key)
                    &&!Set.of("DeviceExtensions","ForgeData","ForgeCaps").contains(key))legacy.put(key,root.get(key).copy());
            for(var entry:REGISTRY.entries()){
                var value=components.get(entry.id());if(value==null)continue;var registration=entry.value();var saved=pending.getCompound(entry.id());
                if(pending.contains(entry.id())&&saved.getInt("Version")!=registration.version()){
                    value.extensionInvalidate();components.remove(entry.id());continue;
                }
                value.extensionLoad(pending.contains(entry.id())?saved.getCompound("Data").copy():legacy.getCompound(registration.legacy()).copy());
            }
        }
        public void save(CompoundTag root){
            ensure();var saved=pending.copy();
            for(String key:legacy.getAllKeys())if(!root.contains(key))root.put(key,legacy.get(key).copy());
            for(var entry:REGISTRY.entries()){
                var value=components.get(entry.id());if(value==null)continue;var registration=entry.value();
                var data=value.extensionSave().copy();var row=new CompoundTag();row.putInt("Version",registration.version());row.put("Data",data);saved.put(entry.id(),row);
                if(!registration.legacy().isEmpty())root.put(registration.legacy(),data.copy());
            }
            if(!saved.isEmpty())root.put("DeviceExtensions",saved);
        }
        public <T> LazyOptional<T> capability(Capability<T> capability,Direction side){
            if(!capsValid||removed||host.isRemoved())return LazyOptional.empty();ensure();
            for(var value:components.values()){var found=value.extensionCapability(capability,side);if(found.isPresent())return found;}
            return LazyOptional.empty();
        }
        public void invalidate(){if(!capsValid)return;capsValid=false;for(var value:components.values())value.extensionInvalidate();}
        public void revive(){if(capsValid&&!removed)return;removed=false;capsValid=true;for(var value:components.values()){value.extensionAttached();value.extensionRevive();}}
        public void beforeRemove(Removal reason){
            if(removed)return;ensure();removed=true;
            for(var value:List.copyOf(components.values()))value.extensionRemoved(reason);invalidate();
        }
        public void unload(){beforeRemove(Removal.UNLOADED);}
    }
}
