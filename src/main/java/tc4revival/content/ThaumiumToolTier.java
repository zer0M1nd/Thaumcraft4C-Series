package tc4revival.content;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Forge's unsorted-tier fallback uses the original numeric harvest level. */
public enum ThaumiumToolTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 400; }
    @Override public float getSpeed() { return 7; }
    @Override public float getAttackDamageBonus() { return 2; }
    @Override public int getLevel() { return 3; }
    @Override public int getEnchantmentValue() { return 22; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.THAUMIUM)); }
}
