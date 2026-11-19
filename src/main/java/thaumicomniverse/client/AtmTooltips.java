package thaumicomniverse.client;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "thaumicomniverse", value = Dist.CLIENT)
public final class AtmTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!ModList.get().isLoaded("allthemodium")) return;
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id != null && id.getNamespace().equals("allthetweaks") &&
                (id.getPath().equals("atm_star") || id.getPath().equals("dragon_soul")))
            event.getToolTip().add(Component.translatable("tooltip.thaumicomniverse.atm_recipe"));
    }
}
