package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Old blank accessories have no worn-tick effect, intrinsic runic charge or vis discount. */
public  class MundaneBaubleItem extends Item implements ICurioItem, RunicGear {
    public MundaneBaubleItem() { super(new Properties().stacksTo(1)); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) { return true; }
    @Override public boolean canUnequip(SlotContext context, ItemStack stack) { return true; }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return false; }
}
