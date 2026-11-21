package ttmodern;

import net.minecraft.world.item.*;
import tc4revival.aspect.*;

public final class InfusedGrainItem extends Item implements thaumcraft.api.aspects.IEssentiaContainerItem {
    private final String aspect;
    public InfusedGrainItem(String aspect) { super(new Properties());this.aspect=aspect; }
    @Override public AspectAmounts storedAspects(ItemStack stack) { return new AspectAmounts().add(TCAspects.catalog().require(aspect),1).add(TCAspects.catalog().require("messis"),1); }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags) { lines.add(net.minecraft.network.chat.Component.literal(thaumcraft.api.aspects.AspectViews.display(TCAspects.catalog().require(aspect)).displayName())); }
}
