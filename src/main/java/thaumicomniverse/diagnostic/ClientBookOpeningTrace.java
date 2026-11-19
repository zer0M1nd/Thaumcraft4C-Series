package thaumicomniverse.diagnostic;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thaumicomniverse", value = Dist.CLIENT)
public final class ClientBookOpeningTrace {
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void before(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof tc4revival.client.ResearchBrowserScreen)
            BookOpeningTrace.log("screen requested", event.getNewScreen().getClass().getName());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void after(ScreenEvent.Opening event) {
        if (tc4revival.client.ClientBookSession.active())
            BookOpeningTrace.log("screen event result", "canceled=" + event.isCanceled() + ", screen="
                    + (event.getNewScreen() == null ? "null" : event.getNewScreen().getClass().getName()));
    }
}
