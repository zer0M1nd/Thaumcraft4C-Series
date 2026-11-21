package conciliummodern;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;

public final class RunicWindings extends ArmorItem implements thaumcraft.api.IRunicArmor,thaumcraft.api.IVisDiscountGear,thaumcraft.api.IWarpingGear {
    private static final ArmorMaterial MATERIAL=new ArmorMaterial(){
        public int getDurabilityForType(Type type){return switch(type){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;};}
        public int getDefenseForType(Type type){return 1;}
        public int getEnchantmentValue(){return 30;}
        public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}
        public String getName(){return ThaumicConcilium.ID+":runic_windings";}
        public float getToughness(){return 0;}
        public float getKnockbackResistance(){return 0;}
    };
    public RunicWindings(Type type){super(MATERIAL,type,new Properties().rarity(Rarity.UNCOMMON));}
    @Override public int getRunicCharge(ItemStack stack){return 200;}
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return 3;}
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer){
        return ThaumicConcilium.ID+":textures/models/armor/"+(slot==EquipmentSlot.CHEST?"chest":"legs")+"_windings.png";
    }
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){
        if(player instanceof ServerPlayer server && player.tickCount%20==0)tc4revival.content.RunicShieldEvents.restore(server,5);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);
        lines.add(net.minecraft.network.chat.Component.translatable("tc.visdiscount").append(": 3%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
}
