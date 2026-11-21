package tc4revival.content;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public  class LegacyHasteEnchantment extends Enchantment {
    /** The hovering harness opts in separately from ordinary chest armor. */
    public interface Harness {}
    public LegacyHasteEnchantment() { super(Rarity.RARE, EnchantmentCategory.ARMOR, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.CHEST}); }
    @Override public int getMaxLevel() { return LegacyEnchantmentRules.Kind.HASTE.maxLevel(); }
    @Override public int getMinCost(int level) { return LegacyEnchantmentRules.Kind.HASTE.minCost(level); }
    @Override public int getMaxCost(int level) { return LegacyEnchantmentRules.Kind.HASTE.maxCost(level); }
    @Override public boolean canEnchant(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.getItem() instanceof ArmorItem armor
                && (armor.getType()==ArmorItem.Type.BOOTS || armor instanceof Harness);
    }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return canEnchant(stack); }
    /** Called by the EnchantmentInstance constructor hook; vanilla rarity enums cannot express 3. */
    public static int selectionWeight(int original, Enchantment enchantment) {
        return enchantment instanceof LegacyHasteEnchantment ? LegacyEnchantmentRules.Kind.HASTE.weight() : original;
    }
}
