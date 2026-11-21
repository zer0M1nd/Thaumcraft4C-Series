package dyesmodern;

import net.minecraft.world.item.*;

public final class DyedArmorItem extends RepairableDyesArmorItem implements DyeableLeatherItem {
    public DyedArmorItem(String family,String legacyKey,ArmorMaterial material,Type type){super(family,legacyKey,material,type);}
    @Override public boolean hasCustomColor(ItemStack stack){return true;}
    @Override public int getColor(ItemStack stack){return super.getColor(stack);}
}
