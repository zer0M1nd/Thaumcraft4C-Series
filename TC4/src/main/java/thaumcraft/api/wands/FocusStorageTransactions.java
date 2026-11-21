package thaumcraft.api.wands;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Synchronous server-thread focus swaps, including storage provenance and inventory rollback. */
public final class FocusStorageTransactions implements AutoCloseable {
    private record Checkpoint(FocusStorageRegistry.Storage storage,List<ItemStack> slots,CompoundTag auxiliary) {}
    private final Inventory inventory;
    private final List<ItemStack> items;
    private final List<Checkpoint> storages;
    private boolean committed;
    public FocusStorageTransactions(Inventory inventory,List<FocusStorageRegistry.Storage> storages){
        this.inventory=inventory;items=new ArrayList<>();
        for(int i=0;i<inventory.getContainerSize();i++)items.add(inventory.getItem(i).copy());
        var checkpoints=new ArrayList<Checkpoint>();
        for(var storage:storages){
            var slots=new ArrayList<ItemStack>();for(int i=0;i<storage.slots();i++)slots.add(storage.get(i).copy());
            checkpoints.add(new Checkpoint(storage,List.copyOf(slots),storage.checkpoint().copy()));
        }
        this.storages=List.copyOf(checkpoints);
    }
    public static boolean returnFocus(List<FocusStorageRegistry.Storage> storages,ItemStack focus){
        if(focus.isEmpty())return true;
        for(var storage:storages){int slot=storage.preferredReturnSlot(focus.copy());if(slot>=0&&storage.insert(slot,focus,true))return storage.insert(slot,focus,false);}
        for(var storage:storages)for(int slot=0;slot<storage.slots();slot++)if(storage.insert(slot,focus,true))return storage.insert(slot,focus,false);
        return false;
    }
    public void commit(){committed=true;}
    @Override public void close(){
        if(committed)return;
        // Restore live wrapper contents before replacing inventory stacks carrying their NBT.
        RuntimeException failure=null;
        for(var checkpoint:storages)try{
            for(int slot=0;slot<checkpoint.slots().size();slot++)
                if(!ItemStack.matches(checkpoint.storage().get(slot),checkpoint.slots().get(slot)))
                    checkpoint.storage().set(slot,checkpoint.slots().get(slot).copy());
            checkpoint.storage().restoreCheckpoint(checkpoint.auxiliary().copy());
        }catch(RuntimeException error){if(failure==null)failure=error;else failure.addSuppressed(error);}
        // A refused swap must not invalidate live handles to stacks that never changed.
        for(int slot=0;slot<items.size();slot++)
            if(!ItemStack.matches(inventory.getItem(slot),items.get(slot)))inventory.setItem(slot,items.get(slot).copy());
        inventory.setChanged();
        if(failure!=null)throw failure;
    }
}
