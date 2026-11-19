package thaumicomniverse.diagnostic;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in observation only: never cancels interactions or changes book authorization. */
@Mod.EventBusSubscriber(modid = "thaumicomniverse")
public final class BookOpeningTrace {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("OmniverseBookTrace");
    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Log Thaumonomicon interaction and screen opening stages for troubleshooting.")
                .comment("记录魔导手册交互和屏幕打开阶段，供故障诊断", "设置说明 / Setting: Log Thauminomicon interaction and screen opening stages for diagnosis", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("traceBookOpening", false);
        SPEC = builder.build();
    }
    public static void setup(){
        thaumcraft.api.research.ResearchBookLifecycle.registerObserver("thaumicomniverse:opening_trace",0,event->log(event.stage().name(),event.detail()));
    }
    public static void log(String stage, Object detail) {
        if (ENABLED.get()) LOG.info("[Omniverse book trace] {}: {}", stage, detail);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void item(PlayerInteractEvent.RightClickItem event) { interaction(event); }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void block(PlayerInteractEvent.RightClickBlock event) { interaction(event); }
    private static void interaction(PlayerInteractEvent event) {
        if (!ENABLED.get() || !(event.getItemStack().getItem() instanceof tc4revival.content.ThaumonomiconItem)) return;
        log(event.getClass().getSimpleName(), "side=" + event.getSide() + ", hand=" + event.getHand()
                + ", canceled=" + event.isCanceled() + ", spectator=" + event.getEntity().isSpectator()
                + ", item=" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()));
    }
}
