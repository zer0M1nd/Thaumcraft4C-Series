package tc4revival.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public enum ThaumiumArmorMaterial implements ArmorMaterial {
    INSTANCE;
    @Override public int getDurabilityForType(ArmorItem.Type type) { return ThaumiumArmorType.from(type).durability(); }
    @Override public int getDefenseForType(ArmorItem.Type type) { return ThaumiumArmorType.from(type).defense(); }
    @Override public int getEnchantmentValue() { return 25; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.THAUMIUM)); }
    @Override public String getName() { return "thaumcraft:thaumium"; }
    @Override public float getToughness() { return 0; }
    @Override public float getKnockbackResistance() { return 0; }
}
