package ttmodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import thaumcraft.api.*;
import thaumcraft.api.aspects.Aspect;

public final class RevealingHelmItem extends ArmorItem implements IRepairable, IGoggles, IVisDiscountGear, IRunicArmor {
    public RevealingHelmItem() { super(tc4revival.content.ThaumiumArmorMaterial.INSTANCE,Type.HELMET,new Properties().durability(500).rarity(Rarity.UNCOMMON)); }
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer) { return true; }
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer) { return true; }
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect) { return 5; }
    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer) { return Tinkerer.ID+":textures/models/revealing.png"; }
}
