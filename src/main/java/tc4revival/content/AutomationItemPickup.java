package tc4revival.content;

import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;

/** Keeps ground items out of TC's shared interaction workers, as in the old pickup event. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID)
public  class AutomationItemPickup {
    private AutomationItemPickup() {}

    @SubscribeEvent public static void pickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof net.minecraftforge.common.util.FakePlayer && event.getEntity().getGameProfile().getName().startsWith("FakeThaumcraft")) event.setCanceled(true);
    }
}
