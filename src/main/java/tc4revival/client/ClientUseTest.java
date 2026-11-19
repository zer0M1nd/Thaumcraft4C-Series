package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.content.TCItems;
import tc4revival.content.CastingWandItem;

/** Retained GUI-triggered test aid: holds the ordinary use binding, never supplies scan results. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientUseTest {
    private static int remaining;
    private static int delay;
    private static boolean holding;
    private ClientUseTest() {}
    public static int schedule(int ticks) {
        if (ticks < 1 || ticks > 40) throw new IllegalArgumentException("Use test must last 1..40 ticks");
        cancel();
        remaining = ticks;
        delay = 5;
        return ticks;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || remaining == 0 && !holding) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive()
                || !(mc.player.getMainHandItem().is(TCItems.THAUMOMETER.get())
                || mc.player.getOffhandItem().is(TCItems.THAUMOMETER.get())
                || mc.player.getMainHandItem().getItem() instanceof CastingWandItem || mc.player.getOffhandItem().getItem() instanceof CastingWandItem)) {
            cancel();
            return;
        }
        if (mc.screen != null) {
            if (holding) cancel();
            return;
        }
        if (delay > 0) { delay--; return; }
        if (remaining == 0) { cancel(); return; }
        mc.options.keyUse.setDown(true);
        holding = true;
        remaining--;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { cancel(); }
    private static void cancel() {
        if (holding) Minecraft.getInstance().options.keyUse.setDown(false);
        remaining = 0;
        delay = 0;
        holding = false;
    }
}
