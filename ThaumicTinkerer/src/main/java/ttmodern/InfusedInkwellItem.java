package ttmodern;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.IScribeTools;

public final class InfusedInkwellItem extends Item implements IScribeTools {
    public InfusedInkwellItem() { super(new Properties().durability(800)); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return tc4revival.content.TCItems.SCRIBING_TOOLS.get().onItemUseFirst(stack,context);
    }
}
