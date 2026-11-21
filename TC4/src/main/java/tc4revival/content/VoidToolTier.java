package tc4revival.content;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum VoidToolTier implements Tier {
    INSTANCE;
    public int getUses() { return 150; }
    public float getSpeed() { return 8; }
    public float getAttackDamageBonus() { return 3; }
    public int getLevel() { return 4; }
    public int getEnchantmentValue() { return 10; }
    public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.VOID)); }
}
