package automagy.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Death notifications survive while the associated core's chunk is unloaded. */
public final class CourierCleanupData extends SavedData {
    private record DeadClaim(BlockPos core,UUID book,long claim,UUID carrier) {}
    private record RemovedTerminal(BlockPos core,UUID book,BlockPos terminal) {}
    private final Set<RemovedTerminal> terminals=new LinkedHashSet<>();
    private final Set<DeadClaim> pending=new LinkedHashSet<>();
    public static CourierCleanupData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(CourierCleanupData::load,CourierCleanupData::new,"automagy_courier_cleanup");}
    public void add(BlockPos core,UUID book,long claim,UUID carrier){if(claim>0&&pending.add(new DeadClaim(core.immutable(),book,claim,carrier)))setDirty();}
    public void removeTerminal(BlockPos core,UUID book,BlockPos terminal){if(terminals.add(new RemovedTerminal(core.immutable(),book,terminal.immutable())))setDirty();}
    public void apply(InventariumBlockEntity core){
        for(var iterator=terminals.iterator();iterator.hasNext();){var removed=iterator.next();if(!removed.core.equals(core.getBlockPos()))continue;
            if(removed.book.equals(core.requests().identity()))core.requests().cancelDestination(net.minecraft.core.GlobalPos.of(core.getLevel().dimension(),removed.terminal));
            iterator.remove();setDirty();
        }
        for(var iterator=pending.iterator();iterator.hasNext();){var dead=iterator.next();if(!dead.core.equals(core.getBlockPos()))continue;
            if(dead.book.equals(core.requests().identity())&&core.requests().owned(dead.claim,dead.carrier))core.requests().fail(dead.claim);
            iterator.remove();setDirty();
        }
    }
    public static CourierCleanupData load(CompoundTag tag){
        var data=new CourierCleanupData();for(Tag raw:tag.getList("DeadClaims",Tag.TAG_COMPOUND)){
            var saved=(CompoundTag)raw;if(saved.contains("Core",Tag.TAG_COMPOUND)&&saved.hasUUID("Book")&&saved.hasUUID("Carrier")&&saved.getLong("Claim")>0)
                data.pending.add(new DeadClaim(NbtUtils.readBlockPos(saved.getCompound("Core")),saved.getUUID("Book"),saved.getLong("Claim"),saved.getUUID("Carrier")));
        }
        for(Tag raw:tag.getList("RemovedTerminals",Tag.TAG_COMPOUND)){var saved=(CompoundTag)raw;
            if(saved.contains("Core",Tag.TAG_COMPOUND)&&saved.hasUUID("Book")&&saved.contains("Terminal",Tag.TAG_COMPOUND))
                data.terminals.add(new RemovedTerminal(NbtUtils.readBlockPos(saved.getCompound("Core")),saved.getUUID("Book"),NbtUtils.readBlockPos(saved.getCompound("Terminal"))));
        }return data;
    }
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();for(var dead:pending){var saved=new CompoundTag();saved.put("Core",NbtUtils.writeBlockPos(dead.core));saved.putUUID("Book",dead.book);saved.putLong("Claim",dead.claim);saved.putUUID("Carrier",dead.carrier);list.add(saved);}tag.put("DeadClaims",list);
        var removed=new ListTag();for(var terminal:terminals){var saved=new CompoundTag();saved.put("Core",NbtUtils.writeBlockPos(terminal.core));saved.putUUID("Book",terminal.book);saved.put("Terminal",NbtUtils.writeBlockPos(terminal.terminal));removed.add(saved);}tag.put("RemovedTerminals",removed);return tag;
    }
}
