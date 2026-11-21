package tc4revival.content;

import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** Both tool and focus contribute, but fortune takes the higher level instead of adding. */
public record ArcaneBoreEquipment(boolean focusPresent,boolean pickPresent,int fortune,int area,int speed,boolean silk,boolean dowsing) {
    public static ArcaneBoreEquipment inspect(ItemStack focus,ItemStack pick) {
        var item=focus.getItem() instanceof ExcavationFocusItem excavation?excavation:null;
        boolean validPick=pick.getItem() instanceof PickaxeItem;
        int fortune=item==null?0:item.upgradeLevel(focus,FocusUpgrade.TREASURE.id);
        int area=item==null?0:item.upgradeLevel(focus,FocusUpgrade.ENLARGE.id);
        int speed=item==null?0:item.upgradeLevel(focus,FocusUpgrade.POTENCY.id);
        if (validPick) {
            fortune=Math.max(fortune,EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,pick));
            speed+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,pick);
        }
        boolean silk=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,pick)>0
                || item!=null && item.upgradeLevel(focus,FocusUpgrade.SILK_TOUCH.id)>0;
        boolean dowsing=pick.getItem() instanceof ElementalPickaxeItem || item!=null && item.upgradeLevel(focus,FocusUpgrade.DOWSING.id)>0;
        return new ArcaneBoreEquipment(item!=null,validPick,fortune,area,speed,silk,dowsing);
    }
    public boolean canDig(ItemStack pick) {
        return focusPresent && pickPresent && (!pick.isDamageableItem() || pick.getDamageValue()<pick.getMaxDamage());
    }
    public int width() { return 1+(2+area)*2; }
}
