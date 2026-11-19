package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;

public  class GogglesItem extends ArmorItem implements RepairableItem, VisDiscountGear, RevealingGear, RunicGear, top.theillusivec4.curios.api.type.capability.ICurioItem {
    public GogglesItem() { super(SpecialArmorMaterial.INSTANCE, Type.HELMET, new Properties().durability(350).rarity(Rarity.RARE)); }
    @Override public boolean canEquip(top.theillusivec4.curios.api.SlotContext context,ItemStack stack) { return tc4revival.config.TCConfig.SERVER.revival.gogglesHeadSlot.get() && context.identifier().equals("head"); }
    @Override public int getMaxDamage(ItemStack stack) { return 350; }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public int visDiscount(ItemStack stack, Player player, Aspect aspect) { return 5; }
    @Override public boolean showNodes(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return material.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack, material); }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) { return "thaumcraft:textures/models/goggles.png"; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
    }
}
