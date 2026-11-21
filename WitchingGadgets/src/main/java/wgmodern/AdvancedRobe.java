package wgmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.*;
import thaumcraft.api.*;
import thaumcraft.api.aspects.Aspect;

public class AdvancedRobe extends ArmorItem implements DyeableLeatherItem,IRepairable,IVisDiscountGear {
    public static class Material implements ArmorMaterial {
        public int getDurabilityForType(Type type){return 25*switch(type){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;};}
        public int getDefenseForType(Type type){return switch(type){case CHESTPLATE->4;case LEGGINGS->3;default->2;};}
        public int getEnchantmentValue(){return 25;}public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.of(Content.MATERIALS.get("cloth_bewitched").get());}
        public String getName(){return "witchinggadgets:advanced_robes";}public float getToughness(){return 0;}public float getKnockbackResistance(){return 0;}
    }
    public AdvancedRobe(Type type){super(new Material(),type,new Properties());}
    @Override public String getDescriptionId(){return getType()==Type.CHESTPLATE?"item.WG_AdvancedRobeChest":"item.WG_AdvancedRobeLegs";}
    @Override public int getColor(ItemStack stack){return hasCustomColor(stack)?DyeableLeatherItem.super.getColor(stack):6961280;}
    @Override public int getVisDiscount(ItemStack stack,Player p,Aspect aspect){return getType()==Type.CHESTPLATE?5:4;}
    @Override public String getArmorTexture(ItemStack stack,Entity e,EquipmentSlot slot,String overlay){return "witchinggadgets:textures/models/advancedrobes_"+(slot==EquipmentSlot.LEGS?2:1)+(overlay==null?"":"_overlay")+".png";}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){return slot==EquipmentSlot.CHEST?RobeModel.prepare(entity,original):original;}});}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tc.visdiscount").append(": "+getVisDiscount(stack,null,null)+"%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
}
