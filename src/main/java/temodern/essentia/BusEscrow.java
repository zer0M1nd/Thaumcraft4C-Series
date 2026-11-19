package temodern.essentia;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-local durable bus buffers. A failed commit never silently voids essentia.
 * Entries survive bus removal and can be recovered using the diagnostic command.
 */
public final class BusEscrow extends SavedData {
    private final Map<String, TransferLedger<String>> entries = new LinkedHashMap<>();
    public static BusEscrow get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BusEscrow::load, BusEscrow::new, "thaumicenergistics_bus_escrow");
    }
    public TransferLedger<String> at(BlockPos target, Direction side, boolean exporting) {
        String address = target.asLong() + ":" + side.getName() + ":" + exporting;
        return entries.computeIfAbsent(address, ignored -> new TransferLedger<>());
    }
    public Map<String, TransferLedger<String>> entries() { return Map.copyOf(entries); }
    private static BusEscrow load(CompoundTag tag) {
        var result = new BusEscrow();
        for (var entry : tag.getList("entries", Tag.TAG_COMPOUND)) {
            var data = (CompoundTag) entry;
            long amount = data.getLong("amount");
            if (amount <= 0) continue;
            result.entries.computeIfAbsent(data.getString("address"), ignored -> new TransferLedger<>())
                    .retain(data.getString("aspect"), amount);
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        entries.forEach((address, ledger) -> ledger.snapshot().forEach((aspect, amount) -> {
            var entry = new CompoundTag();
            entry.putString("address", address); entry.putString("aspect", aspect); entry.putLong("amount", amount);
            list.add(entry);
        }));
        tag.put("entries", list);
        return tag;
    }
}
