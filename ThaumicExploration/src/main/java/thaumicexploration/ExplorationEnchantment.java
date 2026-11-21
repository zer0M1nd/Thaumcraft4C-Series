package thaumicexploration;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.*;

public class ExplorationEnchantment extends Enchantment {
    private final String kind;
    public ExplorationEnchantment(String kind) {
        super(Rarity.VERY_RARE, kind.equals("night_vision") ? EnchantmentCategory.ARMOR_HEAD : EnchantmentCategory.WEAPON,
                kind.equals("night_vision") ? new EquipmentSlot[]{EquipmentSlot.HEAD} : new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.kind = kind;
    }
    @Override public int getMaxLevel() { return kind.equals("night_vision") ? 1 : kind.equals("binding") ? 2 : 3; }
    @Override public boolean isTradeable() { return false; }
    @Override public boolean isDiscoverable() { return false; }
    @Override public boolean isAllowedOnBooks() { return false; }
    @Override public int getMinCost(int level) { return kind.equals("night_vision")?40:10+20*(level-1); }
    @Override public boolean canApplyAtEnchantingTable(net.minecraft.world.item.ItemStack stack){return false;}
    @Override public boolean canEnchant(net.minecraft.world.item.ItemStack stack){return category.canEnchant(stack.getItem());}
    @Override protected boolean checkCompatibility(Enchantment other){
        if(!super.checkCompatibility(other))return false;
        if(kind.equals("binding")&&net.minecraft.resources.ResourceLocation.parse("thaumic_tinkerer:vampirism").equals(net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(other)))return false;
        return switch(kind){case "binding"->other!=Enchantments.FIRE_ASPECT;case "disarm"->other!=Enchantments.MOB_LOOTING;default->other!=Enchantments.RESPIRATION&&other!=Enchantments.AQUA_AFFINITY;};
    }
}
