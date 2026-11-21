package tc4revival.content;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum ElementalToolTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 1500; }
    @Override public float getSpeed() { return 10; }
    @Override public float getAttackDamageBonus() { return 3; }
    @Override public int getLevel() { return 3; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.THAUMIUM)); }
}
