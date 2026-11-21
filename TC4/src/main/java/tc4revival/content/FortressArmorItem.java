package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** The goggles upgrade reveals nodes but does not inherit the separate goggles' vis discount. */
public  class FortressArmorItem extends ArmorItem implements RepairableItem,RunicGear,RevealingGear,WarpEvents.MaskProtection {
    public FortressArmorItem(Type type) { super(CultistEquipment.FortressMaterial.INSTANCE,type,new Properties().rarity(Rarity.RARE)); }
    public static boolean hasGoggles(ItemStack stack) { return stack.hasTag() && stack.getTag().contains("goggles"); }
    public static int mask(ItemStack stack) { return stack.hasTag() && stack.getTag().contains("mask") ? stack.getTag().getInt("mask") : -1; }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public boolean protectsFromWarp(ItemStack stack) { return mask(stack)==0; }
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer) { return hasGoggles(stack); }
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer) { return hasGoggles(stack); }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient) { return ingredient.is(TCItems.material(MaterialType.THAUMIUM)); }
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer) { return "thaumcraft:textures/models/fortress_armor.png"; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new tc4revival.client.FortressArmorExtensions());
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        if (hasGoggles(stack)) lines.add(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));
        if (stack.hasTag() && stack.getTag().contains("mask")) lines.add(Component.translatable("item.HelmetFortress.mask."+mask(stack)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack,level,lines,flags);
    }
}
