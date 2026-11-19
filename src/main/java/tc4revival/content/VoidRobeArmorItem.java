package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import tc4revival.aspect.Aspect;

public  class VoidRobeArmorItem extends ArmorItem implements DyeableLeatherItem,RepairableItem,RunicGear,VisDiscountGear,WarpingGear,RevealingGear {
    private enum Material implements ArmorMaterial {
        INSTANCE;
        // ConfigItems actually constructs the robes with VOID, not the unused VOIDFORTRESS material.
        public int getDurabilityForType(Type type) { return 10*switch(type) { case HELMET->11; case CHESTPLATE->16; case LEGGINGS->15; case BOOTS->13; }; }
        public int getDefenseForType(Type type) { return switch(type) { case HELMET,BOOTS->3; case CHESTPLATE->7; case LEGGINGS->6; }; }
        public int getEnchantmentValue() { return 10; }
        public net.minecraft.sounds.SoundEvent getEquipSound() { return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER; }
        public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.VOID)); }
        public String getName() { return "thaumcraft:void_robe"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    }
    public VoidRobeArmorItem(Type type) { super(Material.INSTANCE,type,new Properties().rarity(Rarity.EPIC)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.EPIC; }
    @Override public boolean hasCustomColor(ItemStack stack) { return true; }
    @Override public int getColor(ItemStack stack) { var display=stack.getTagElement("display"); return display!=null && display.contains("color")?display.getInt("color"):RobeArmorItem.DEFAULT_COLOR; }
    @Override public int visDiscount(ItemStack stack,Player player,Aspect aspect) { return 5; }
    @Override public int warp(ItemStack stack,Player player) { return 2; }
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer) { return getType()==Type.HELMET; }
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer) { return getType()==Type.HELMET; }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected) {
        if (!level.isClientSide && !(holder instanceof Player player && player.getItemBySlot(getEquipmentSlot())==stack)) VoidEquipment.repair(stack,holder);
    }
    @Override public void onArmorTick(ItemStack stack,Level level,Player player) { if (!level.isClientSide) VoidEquipment.repair(stack,player); }
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer) { return "thaumcraft:textures/models/void_robe_armor"+(layer==null?"_overlay":"")+".png"; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) { consumer.accept(new tc4revival.client.VoidRobeExtensions()); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var state=level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!level.isClientSide) { clearColor(stack); LayeredCauldronBlock.lowerFillLevel(state,level,pos); level.updateNeighbourForOutputSignal(pos,state.getBlock()); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
         super.appendHoverText(stack,level,lines,flags);
    }
}
