package tc4revival.content;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import thaumcraft.api.wands.WandAutomation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class WandAutomationEvents {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof CastingWandItem && !WandAutomation.allowed(event.getItemStack(),event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void rightItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() instanceof CastingWandItem && !WandAutomation.allowed(event.getItemStack(),event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var stack=event.getEntity().getMainHandItem();
        if (stack.getItem() instanceof CastingWandItem && !WandAutomation.allowed(stack,event.getEntity())) event.setCanceled(true);
    }
}
