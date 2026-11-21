package tc4revival.research;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.ItemStackAspects;

/** Keep existing legacy identities; modern-only objects receive stable, disjoint registry identities. */
public  class ScanIdentity {
    public static final String EMPTY_ICON="_empty";
    private ScanIdentity() {}
    public static String item(ItemStack stack) {
        return item(stack,tc4revival.aspect.item.ItemAspectAccess.SERVER);
    }
    public static String item(ItemStack stack,tc4revival.aspect.item.ItemAspectAccess access) {
        var legacy=ItemStackAspects.scanHash(stack,access);
        return legacy.isPresent()?Integer.toString(legacy.getAsInt()):"modern:item:"+ForgeRegistries.ITEMS.getKey(stack.getItem());
    }
}
