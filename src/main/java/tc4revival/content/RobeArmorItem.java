package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import tc4revival.aspect.Aspect;

public  class RobeArmorItem extends ArmorItem implements DyeableLeatherItem, RepairableItem, VisDiscountGear, RunicGear {
    public static final int DEFAULT_COLOR = 6961280;
    public RobeArmorItem(Type type) { super(SpecialArmorMaterial.INSTANCE, type, new Properties().rarity(Rarity.UNCOMMON)); }
    @Override public boolean hasCustomColor(ItemStack stack) { return true; }
    @Override public int getColor(ItemStack stack) {
        var display = stack.getTagElement("display"); return display != null && display.contains("color", 99) ? display.getInt("color") : DEFAULT_COLOR;
    }
    @Override public int visDiscount(ItemStack stack, Player player, Aspect aspect) { return getType() == Type.BOOTS ? 1 : 2; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return material.is(TCItems.material(MaterialType.FABRIC)) || super.isValidRepairItem(stack, material); }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) {
        return "thaumcraft:textures/models/robes_" + (getType() == Type.LEGGINGS ? 2 : 1) + (layer == null ? "" : "_overlay") + ".png";
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {

    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if (!state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            clearColor(stack); LayeredCauldronBlock.lowerFillLevel(state, context.getLevel(), context.getClickedPos());
            context.getLevel().updateNeighbourForOutputSignal(context.getClickedPos(), state.getBlock());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
