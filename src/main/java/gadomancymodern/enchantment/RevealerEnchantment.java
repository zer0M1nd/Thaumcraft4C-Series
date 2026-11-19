package gadomancymodern.enchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
public class RevealerEnchantment extends Enchantment {
    public RevealerEnchantment(){super(Rarity.VERY_RARE,EnchantmentCategory.ARMOR_HEAD,new EquipmentSlot[]{EquipmentSlot.HEAD});}
    @Override public String getDescriptionId(){return "gadomancy.enchantment.revealer";}
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack){return false;}
    // Forge's modern canEnchant delegates to the enchanting-table hook. Infusion
    // still needs the legacy helmet category check even though table rolls reject it.
    @Override public boolean canEnchant(ItemStack stack){return !stack.isEmpty()&&category.canEnchant(stack.getItem());}
    @Override public int getMinCost(int level){return 15;}
    @Override public int getMaxCost(int level){return super.getMinCost(level)+50;}
    public static boolean active(net.minecraft.world.entity.LivingEntity entity){return entity.getItemBySlot(EquipmentSlot.HEAD).getEnchantmentLevel(gadomancymodern.Gadomancy.REVEALER.get())>0;}
}
