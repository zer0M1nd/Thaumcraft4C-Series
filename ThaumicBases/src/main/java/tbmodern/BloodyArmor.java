package tbmodern;

import java.util.UUID;
import com.google.common.collect.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.*;
import thaumcraft.api.aspects.Aspect;

public final class BloodyArmor extends ArmorItem implements IRepairable,IVisDiscountGear,IRunicArmor {
    private static final ArmorMaterial MATERIAL=new ArmorMaterial() {
        public int getDurabilityForType(Type type) { return (switch(type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; })*21; }
        public int getDefenseForType(Type type) { return switch(type) { case HELMET,BOOTS -> 2; case CHESTPLATE -> 6; case LEGGINGS -> 5; }; }
        public int getEnchantmentValue() { return 21; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        public Ingredient getRepairIngredient() { return Ingredient.of(Content.item("bloody_cloth")); }
        public String getName() { return "thaumicbases:bloody"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    };
    private final Multimap<Attribute,AttributeModifier> modifiers;
    public BloodyArmor(Type type) {
        super(MATERIAL,type,new Item.Properties().rarity(Rarity.UNCOMMON));
        var values=ImmutableMultimap.<Attribute,AttributeModifier>builder();values.putAll(super.getDefaultAttributeModifiers(type.getSlot()));
        switch(type) {
            case CHESTPLATE -> values.put(Attributes.MAX_HEALTH,new AttributeModifier(UUID.fromString("96042c45-dfe3-4366-b93b-84663c4d828d"),"Bloody health",.2,AttributeModifier.Operation.MULTIPLY_TOTAL));
            case LEGGINGS -> values.put(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(UUID.fromString("e4e1d8b2-87f2-44f5-8f24-e1876060a04c"),"Bloody knockback",.5,AttributeModifier.Operation.ADDITION));
            case BOOTS -> values.put(Attributes.MOVEMENT_SPEED,new AttributeModifier(UUID.fromString("f6d1384c-74c3-4cce-9a80-11b91dbd4ff4"),"Bloody speed",.1,AttributeModifier.Operation.MULTIPLY_TOTAL));
            default -> { }
        }
        modifiers=values.build();
    }
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot==getType().getSlot()?modifiers:super.getDefaultAttributeModifiers(slot);
    }
    public int getVisDiscount(ItemStack stack,Player player,Aspect aspect) { return switch(getType()) { case HELMET -> 5; case CHESTPLATE -> 4; case LEGGINGS -> 3; case BOOTS -> 2; }; }
    public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String type) {
        return "thaumicbases:textures/armor/bloody_"+(slot==EquipmentSlot.LEGS?"2":"1")+".png";
    }
}
