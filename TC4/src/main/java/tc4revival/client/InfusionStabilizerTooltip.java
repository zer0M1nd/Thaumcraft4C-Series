package tc4revival.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.InfusionLayout;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public class InfusionStabilizerTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.is(InfusionLayout.STABILIZER_ITEMS) || stack.getItem() instanceof BlockItem block && block.getBlock().defaultBlockState().is(InfusionLayout.STABILIZERS))
            event.getToolTip().add(Component.translatable("thaumcraft.tooltip.infusion_stabilizer").withStyle(style -> style.withColor(0xffa500)));
    }
}
