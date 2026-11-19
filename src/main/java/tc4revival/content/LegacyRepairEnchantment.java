package tc4revival.content;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

/** Repair opts into eligible TC4 equipment and uses the inventory vis payment path. */
public  class LegacyRepairEnchantment extends Enchantment {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> REPAIRABLE=net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:repairable"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> HOTBAR=net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:repairable_in_hotbar"));
    public LegacyRepairEnchantment() { super(Rarity.RARE, EnchantmentCategory.BREAKABLE, EquipmentSlot.values()); }
    @Override public int getMaxLevel() { return LegacyEnchantmentRules.Kind.REPAIR.maxLevel(); }
    @Override public int getMinCost(int level) { return LegacyEnchantmentRules.Kind.REPAIR.minCost(level); }
    @Override public int getMaxCost(int level) { return LegacyEnchantmentRules.Kind.REPAIR.maxCost(level); }
    @Override public boolean canEnchant(ItemStack stack) { return stack.isDamageableItem() && stack.is(REPAIRABLE); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return canEnchant(stack); }
    @Override protected boolean checkCompatibility(Enchantment other) { return super.checkCompatibility(other) && other != Enchantments.UNBREAKING; }
}
