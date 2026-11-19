package tc4revival.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;

/** Whole percentage points, before the wand cap and focus modifiers. */
public interface VisDiscountGear {
    int visDiscount(ItemStack stack, Player wearer, Aspect aspect);
}
