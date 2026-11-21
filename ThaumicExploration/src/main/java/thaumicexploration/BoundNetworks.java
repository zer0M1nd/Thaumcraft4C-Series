package thaumicexploration;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.EssentiaBuffer;

/** One persistent authority per server, including networks with no loaded endpoints. */
public final class BoundNetworks extends SavedData {
    private final Map<String,SimpleContainer> chests = new HashMap<>();
    private final Map<String,EssentiaBuffer> jars = new HashMap<>();
    public static BoundNetworks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(BoundNetworks::load, BoundNetworks::new, "thaumicexploration_networks");
    }
    public SimpleContainer chest(String key) {
        return chests.computeIfAbsent(key, ignored -> new SimpleContainer(27) {
            @Override public void setChanged() { super.setChanged(); BoundNetworks.this.setDirty(); }
        });
    }
    public EssentiaBuffer jar(String key) {
        return jars.computeIfAbsent(key, ignored -> new EssentiaBuffer(64, a -> {
            var current = jars.get(key);
            return current == null || current.contents().total() == 0 || current.contents().amount(a) > 0;
        }, this::setDirty));
    }
    public static BoundNetworks load(CompoundTag tag) {
        var data = new BoundNetworks();
        for (Tag entry : tag.getList("chests", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)entry;
            var inventory = data.chest(row.getString("key"));
            for (Tag item : row.getList("items", Tag.TAG_COMPOUND)) {
                var value = (CompoundTag)item; int slot = value.getInt("slot");
                if (slot >= 0 && slot < 27) inventory.setItem(slot, ItemStack.of(value.getCompound("stack")));
            }
        }
        for (Tag entry : tag.getList("jars", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)entry; data.jar(row.getString("key")).load(row.getCompound("contents"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var chestRows = new ListTag();
        chests.forEach((key, inventory) -> {
            var row = new CompoundTag(); row.putString("key", key); var items = new ListTag();
            for (int i=0; i<27; i++) if (!inventory.getItem(i).isEmpty()) {
                var item = new CompoundTag(); item.putInt("slot", i); item.put("stack", inventory.getItem(i).save(new CompoundTag())); items.add(item);
            }
            row.put("items", items); chestRows.add(row);
        });
        tag.put("chests", chestRows); var jarRows = new ListTag();
        jars.forEach((key, contents) -> { var row = new CompoundTag(); row.putString("key", key); row.put("contents", contents.save()); jarRows.add(row); });
        tag.put("jars", jarRows); return tag;
    }
}
