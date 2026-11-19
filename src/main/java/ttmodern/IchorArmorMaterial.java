package ttmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.*;

public final class IchorArmorMaterial implements ArmorMaterial {
    public static final IchorArmorMaterial INSTANCE=new IchorArmorMaterial();
    public int getDurabilityForType(ArmorItem.Type type){return 0;}
    public int getDefenseForType(ArmorItem.Type type){return switch(type){case HELMET,BOOTS->3;case CHESTPLATE->8;case LEGGINGS->6;};}
    public int getEnchantmentValue(){return 20;}
    public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
    public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}
    public String getName(){return Tinkerer.ID+":ichorcloth";}
    public float getToughness(){return 0;}
    public float getKnockbackResistance(){return 0;}
    private IchorArmorMaterial(){}
}
