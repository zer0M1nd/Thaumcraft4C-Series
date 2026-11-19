package emtmodern.item;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class ElectricWings extends ic2.core.item.armor.ItemArmorElectric implements WingEquipment,thaumcraft.api.IVisDiscountGear,thaumcraft.api.IRepairable {
    protected final WingFlight.Profile profile;
    public ElectricWings(String kind){super(ElectricGoggles.MATERIAL,EquipmentSlot.CHEST,new Properties().stacksTo(1),kind.equals("nano")?100000:1000000,kind.equals("nano")?1000:10000,2);profile=WingFlight.PROFILES.get(kind);}
    @Override public WingFlight.Profile wingProfile(){return profile;}
    @Override public int getEnergyPerDamage(){return profile.id().equals("nano")?100:1000;}
    @Override public double getDamageAbsorptionRatio(){return 1.1;}
    // Original wings deliberately use the helmet ratio despite occupying the chest slot.
    @Override public double getBaseAbsorptionRatio(){return .15;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return profile.discount();}
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){WingFlight.tick(player,stack,profile);}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "emt:textures/models/"+profile.id()+"wing.png";}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new emtmodern.client.WingArmorExtensions());}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){super.appendHoverText(stack,level,lines,flags);lines.add(net.minecraft.network.chat.Component.translatable("tooltip.EMT.visDiscount").append(": "+profile.discount()+"%"));}
}
