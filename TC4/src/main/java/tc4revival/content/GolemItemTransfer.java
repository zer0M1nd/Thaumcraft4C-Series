package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

/** Side-bound native item handlers and stacked insertion. */
public  class GolemItemTransfer {
    private GolemItemTransfer() {}
    public static IItemHandler at(ServerLevel level,BlockPos pos,Direction face) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return null;
        var entity=level.getBlockEntity(pos);
        return entity==null?null:entity.getCapability(ForgeCapabilities.ITEM_HANDLER,face).resolve().orElse(null);
    }
    public static ItemStack insert(IItemHandler inventory,ItemStack input,boolean execute) {
        return inventory==null || input.isEmpty()?input.copy():net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory,input.copy(),!execute);
    }
    public static ItemStack insert(ServerLevel level,BlockPos pos,Direction face,ItemStack input,boolean execute) {
        return insert(GolemInventoryViews.single(level,pos,face),input,execute);
    }
    public static ItemStack first(IItemHandler inventory,int amount,boolean execute) {
        if (inventory==null || amount<=0) return ItemStack.EMPTY;
        for (int slot=0;slot<inventory.getSlots();slot++) {
            var extracted=inventory.extractItem(slot,amount,!execute);
            if (!extracted.isEmpty()) return extracted;
        }
        return ItemStack.EMPTY;
    }
    public static ItemStack extract(IItemHandler inventory,ItemStack filter,int amount,GolemConfiguration config,boolean execute) {
        if (inventory==null || filter.isEmpty() || amount<=0) return ItemStack.EMPTY;
        for (int slot=0;slot<inventory.getSlots();slot++) {
            if (!GolemItemMatching.matches(inventory.getStackInSlot(slot),filter,config)) continue;
            var extracted=inventory.extractItem(slot,amount,!execute);
            if (!extracted.isEmpty()) return extracted;
        }
        return ItemStack.EMPTY;
    }
}
