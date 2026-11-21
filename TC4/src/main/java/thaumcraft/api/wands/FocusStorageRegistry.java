package thaumcraft.api.wands;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.FocusPouchItem;
import tc4revival.content.WandFocusItem;

/** Register on both sides during setup. Providers bind to the live stack, never a cached NBT snapshot.
 * Writes must complete synchronously; a storage slot holds exactly one focus. */
public class FocusStorageRegistry {
    public interface Storage {
        int slots();
        ItemStack get(int slot);
        void set(int slot, ItemStack value);
        /** Preferred original location, or -1. Querying and simulating must not change storage. */
        default int preferredReturnSlot(ItemStack value){return -1;}
        /** Extra provenance/settings changed by set(), for an atomic focus swap rollback. */
        default net.minecraft.nbt.CompoundTag checkpoint(){return new net.minecraft.nbt.CompoundTag();}
        default void restoreCheckpoint(net.minecraft.nbt.CompoundTag state){}
        default boolean insert(int slot,ItemStack value,boolean simulate){
            if(slot<0||slot>=slots()||!get(slot).isEmpty()||!accepts(slot,value.copy()))return false;
            if(!simulate){set(slot,value.copy());if(!ItemStack.matches(get(slot),value))throw new IllegalStateException("Focus storage rejected committed insertion");}
            return true;
        }
        default boolean accepts(int slot, ItemStack value) {
            return value.isEmpty() || value.getCount()==1 && value.getItem() instanceof WandFocusItem;
        }
    }
    private static final Map<ResourceLocation,Function<ItemStack,Storage>> PROVIDERS=new TreeMap<>();
    private FocusStorageRegistry() {}
    public static synchronized void register(ResourceLocation id, Function<ItemStack,Storage> provider) {
        if(PROVIDERS.putIfAbsent(id,java.util.Objects.requireNonNull(provider))!=null)
            throw new IllegalArgumentException("Duplicate focus storage "+id);
    }
    public static synchronized Optional<Storage> find(ItemStack stack) {
        if(stack.isEmpty())return Optional.empty();
        for(var provider:PROVIDERS.values()) {
            var storage=provider.apply(stack);
            if(storage!=null) {
                if(storage.slots()<0 || storage.slots()>4096)throw new IllegalArgumentException("Focus storage size outside 0..4096");
                return Optional.of(storage);
            }
        }
        if(!(stack.getItem() instanceof FocusPouchItem))return Optional.empty();
        return Optional.of(new Storage() {
            public int slots(){return FocusPouchItem.contents(stack).size();}
            public ItemStack get(int slot){return FocusPouchItem.contents(stack).get(slot).copy();}
            public void set(int slot,ItemStack value){var contents=FocusPouchItem.contents(stack);contents.set(slot,value.copy());FocusPouchItem.save(stack,contents);}
        });
    }
}
