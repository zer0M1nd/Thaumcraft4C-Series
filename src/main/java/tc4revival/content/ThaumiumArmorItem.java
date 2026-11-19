package tc4revival.content;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** The original ordinary biped armor, without a custom model or an intrinsic runic shield. */
public  class ThaumiumArmorItem extends ArmorItem implements RepairableItem, RunicGear {
    public ThaumiumArmorItem(ThaumiumArmorType type) {
        super(ThaumiumArmorMaterial.INSTANCE, type.type(), new Properties().rarity(Rarity.UNCOMMON));
    }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) {
        return "thaumcraft:textures/models/thaumium_" + (getType() == Type.LEGGINGS ? "2" : "1") + ".png";
    }
}
