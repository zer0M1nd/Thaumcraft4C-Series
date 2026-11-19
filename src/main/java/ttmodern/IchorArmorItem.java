package ttmodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import thaumcraft.api.*;
import thaumcraft.api.aspects.Aspect;

public class IchorArmorItem extends ArmorItem implements IVisDiscountGear, IRunicArmor, IWarpingGear {
    public IchorArmorItem(Type type){super(IchorArmorMaterial.INSTANCE,type,new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect){return getType()==Type.BOOTS?3:4;}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    public float absorption(){return getDefense()*.0425f;}
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer){return Tinkerer.ID+":textures/model/ichor"+(slot==EquipmentSlot.LEGS?"2":"1")+".png";}
}
