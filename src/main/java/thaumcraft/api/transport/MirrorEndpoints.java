package thaumcraft.api.transport;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import tc4revival.content.ItemMirrorBlockEntity;
import tc4revival.content.ItemMirrorItem;

/** Stable mirror identities let movable hosts retain links through unloading and placement. */
public class MirrorEndpoints extends SavedData {
    private final Map<UUID,GlobalPos> positions=new HashMap<>();
    private final Map<UUID,WeakReference<ItemMirrorBlockEntity>> loaded=new HashMap<>();
    private final Set<UUID> destroyed=new HashSet<>();
    public static MirrorEndpoints get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(MirrorEndpoints::load,MirrorEndpoints::new,"thaumcraft_mirror_endpoints");}
    public void register(ItemMirrorBlockEntity mirror,GlobalPos pos){
        destroyed.remove(mirror.identity());
        loaded.put(mirror.identity(),new WeakReference<>(mirror));
        if(!pos.equals(positions.put(mirror.identity(),pos)))setDirty();
    }
    public void unregister(ItemMirrorBlockEntity mirror){var ref=loaded.get(mirror.identity());if(ref!=null&&ref.get()==mirror)loaded.remove(mirror.identity());}
    public void destroy(ItemMirrorBlockEntity mirror){unregister(mirror);destroyed.add(mirror.identity());positions.remove(mirror.identity());setDirty();}
    public boolean destroyed(UUID id){return destroyed.contains(id);}
    public ItemMirrorBlockEntity resolve(MinecraftServer server,UUID id){
        var reference=loaded.get(id);var mirror=reference==null?null:reference.get();
        if(mirror!=null&&mirror.endpointLive())return mirror;
        var pos=positions.get(id);if(pos==null)return null;
        var level=server.getLevel(pos.dimension());if(level==null)return null;
        var tile=level.getBlockEntity(pos.pos());
        return tile instanceof ItemMirrorBlockEntity result&&id.equals(result.identity())?result:null;
    }
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();positions.forEach((id,pos)->{var entry=new CompoundTag();entry.putUUID("Identity",id);ItemMirrorItem.writeDestination(entry,pos,"");list.add(entry);});tag.put("Mirrors",list);
        var removed=new ListTag();destroyed.forEach(id->{var entry=new CompoundTag();entry.putUUID("Identity",id);removed.add(entry);});tag.put("Destroyed",removed);return tag;
    }
    private static MirrorEndpoints load(CompoundTag tag){
        var data=new MirrorEndpoints();for(var value:tag.getList("Mirrors",Tag.TAG_COMPOUND)){var entry=(CompoundTag)value;var pos=ItemMirrorItem.readDestination(entry);if(pos!=null&&entry.hasUUID("Identity"))data.positions.put(entry.getUUID("Identity"),pos);}
        for(var value:tag.getList("Destroyed",Tag.TAG_COMPOUND)){var entry=(CompoundTag)value;if(entry.hasUUID("Identity"))data.destroyed.add(entry.getUUID("Identity"));}return data;
    }
    public interface MobileHost {
        net.minecraft.world.phys.Vec3 position();
        net.minecraft.world.phys.Vec3 outward();
        boolean valid();
        void changed(ItemMirrorBlockEntity mirror);
        /** Return -1 for ordinary ejection, otherwise the committed accepted count. */
        default int transferOutput(net.minecraft.world.item.ItemStack offered){return -1;}
        default void transfer(){}
    }
}
