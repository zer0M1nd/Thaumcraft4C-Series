package dyesmodern;

import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import tc4revival.content.*;

/** Values are in legacy helmet/chest/legs/boots order, as in ItemHandler 1.10.5. */
public enum DyesMaterial implements ArmorMaterial {
    RobesArmor(15,15,2,6,5,1),KnightArmor(20,12,2,6,5,1),FortressArmor(45,30,3,7,6,1),
    IronFortressArmor(25,12,2,6,5,2),RunicArmor(0,25,1,3,2,1),RunicII(0,25,2,6,5,2),RunicIII(0,25,3,7,6,3),
    IRON(15,9,2,6,5,2),armorMatThaumium(25,25,2,6,5,2),armorMatVoid(10,10,3,7,6,3),armorMatSpecial(25,25,1,3,2,1);
    private final int durability,enchant;private final int[] defense;
    DyesMaterial(int durability,int enchant,int...defense){this.durability=durability;this.enchant=enchant;this.defense=defense;}
    public static int legacySlot(ArmorItem.Type type){return switch(type){case HELMET->0;case CHESTPLATE->1;case LEGGINGS->2;case BOOTS->3;};}
    public int getDurabilityForType(ArmorItem.Type type){return durability*new int[]{11,16,15,13}[legacySlot(type)];}
    public int getDefenseForType(ArmorItem.Type type){return defense[legacySlot(type)];}
    public int getEnchantmentValue(){return enchant;}
    public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_IRON;}
    public Ingredient getRepairIngredient(){return switch(this){case armorMatThaumium,FortressArmor->Ingredient.of(TCItems.material(MaterialType.THAUMIUM));case armorMatVoid->Ingredient.of(TCItems.material(MaterialType.VOID));default->Ingredient.of(Items.IRON_INGOT);};}
    public String getName(){return "thaumicdyes:"+name().toLowerCase(java.util.Locale.ROOT);}
    public float getToughness(){return 0;}
    public float getKnockbackResistance(){return 0;}
}
