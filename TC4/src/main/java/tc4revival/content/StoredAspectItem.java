package tc4revival.content;

import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Modern counterpart of the old essentia-container item contract, retaining the Aspects tag layout. */
public interface StoredAspectItem {
    default AspectAmounts storedAspects(ItemStack stack) {
        return stack.hasTag() ? AspectNbt.read(stack.getTag(), TCAspects.catalog()).amounts() : new AspectAmounts();
    }
    /** Fixed-content items retain their aspects; mutable bulk storage may opt out of scan rewards. */
    default AspectAmounts scanAspects(ItemStack stack) { return storedAspects(stack); }
    default void setStoredAspects(ItemStack stack, AspectAmounts aspects) {
        AspectNbt.write(stack.getOrCreateTag(), aspects);
    }
}
