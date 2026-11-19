package tc4revival.content;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** InventoryUtils.areItemStacksEqual ordering, with legacy material categories backed by tags. */
public  class GolemItemMatching {
    private GolemItemMatching() {}
    public static boolean matches(ItemStack offered,ItemStack expected,GolemConfiguration config) {
        return matches(offered,expected,config.toggle(5),config.toggle(6),config.toggle(7));
    }
    public static boolean matches(ItemStack offered,ItemStack expected,boolean materials,boolean ignoreWear,boolean ignoreNbt) {
        if (offered.isEmpty() || expected.isEmpty()) return offered.isEmpty() && expected.isEmpty();
        if (materials && LegacyCraftingCategories.matches(offered,expected)) return true;
        if (offered.getItem()!=expected.getItem()) return false;
        boolean sameDamage=offered.getDamageValue()==expected.getDamageValue();
        if (ignoreWear && ((offered.isDamageableItem() && expected.isDamageableItem())
                || offered.getDamageValue()==32767 || expected.getDamageValue()==32767)) sameDamage=true;
        return sameDamage && (ignoreNbt || Objects.equals(legacyTag(offered),legacyTag(expected)));
    }
    private static CompoundTag legacyTag(ItemStack stack) {
        if (!stack.hasTag()) return null;
        var result=stack.getTag().copy();
        // Modern durability moved inside the tag; TC4 compared it independently above.
        if (stack.isDamageableItem()) { result.remove("Damage"); if (result.isEmpty()) return null; }
        return result;
    }
}
