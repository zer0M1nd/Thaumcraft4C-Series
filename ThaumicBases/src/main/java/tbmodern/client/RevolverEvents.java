package tbmodern.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tbmodern.Revolver;
import tbmodern.api.RevolverUpgrades;

@Mod.EventBusSubscriber(modid="thaumicbases",value=Dist.CLIENT)
public final class RevolverEvents {
    @SubscribeEvent public static void zoom(ComputeFovModifierEvent event) {
        var player=event.getPlayer();
        if(player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof Revolver)
            event.setNewFovModifier(.9F/(RevolverUpgrades.level(player.getMainHandItem(),"accuracy")+1));
    }
    @SubscribeEvent public static void crosshair(RenderGuiOverlayEvent.Pre event) {
        var player=Minecraft.getInstance().player;
        if(player!=null && player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof Revolver && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()))event.setCanceled(true);
    }
}
