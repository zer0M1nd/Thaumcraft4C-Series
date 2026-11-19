package heartwork.compat.blade;

import java.util.List;
import mods.flammpfeil.slashblade.item.ItemSlashBladeDetune;
import mods.flammpfeil.slashblade.capability.slashblade.NamedBladeStateCapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class BasicBladeItem extends ItemSlashBladeDetune {
    private final boolean model;
    public BasicBladeItem(boolean model) {super(Tiers.IRON,4,0,new Properties().stacksTo(1));this.model=model;}
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt) {return new NamedBladeStateCapabilityProvider(stack);}
    @Override public void onCraftedBy(ItemStack stack,Level level,Player player) {
        super.onCraftedBy(stack,level,player);stack.getOrCreateTag().putString("HeartworkCrafter",player.getGameProfile().getName());
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material) {
        return material.is(ItemTags.create(new ResourceLocation("forge:ingots/steel")))||material.is(ItemTags.create(new ResourceLocation("forge:nuggets/steel")))||super.isValidRepairItem(stack,material);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable(model?"blades.ps.model":"blades.ps"));super.appendHoverText(stack,level,lines,flag);
        if(stack.hasTag()&&stack.getTag().contains("HeartworkCrafter"))lines.add(Component.translatable("blades.crafter").append(": "+stack.getTag().getString("HeartworkCrafter")));
    }
}
