package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.network.ScanFeedbackPacket;
import tc4revival.research.ScanRules;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientScanFeedback {
    private static final NotificationTimeline TIMELINE = new NotificationTimeline();
    private static long nextPoolSound;
    private static final ResourceLocation BOOK = ResourceLocation.parse("thaumcraft:textures/items/thaumonomicon.png");
    private static final ResourceLocation PARTICLES = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private ClientScanFeedback() {}
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("notifications", (gui, graphics, partial, width, height) -> {
            if (!(Minecraft.getInstance().screen instanceof ResearchTableScreen)) render(graphics, width, height);
        });
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { TIMELINE.clear(); nextPoolSound = 0; }
    public static void receiveClue(tc4revival.network.CluePacket packet, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source || !source.isConnected()) return;
        add("\u00a7a" + I18n.get("tc.addclue"), null, 0xffffff);
        mc.player.playSound(tc4revival.content.TCSounds.LEARN.get(), 0.2f, 1.0f + mc.player.getRandom().nextFloat() * 0.1f);
    }
    public static void receive(ScanFeedbackPacket packet, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source || !source.isConnected()) return;
        if (packet.status() == ScanRules.Status.NO_ASPECTS) add(I18n.get("tc.unknownobject"), null, 0xffffff);
        else if (packet.status() == ScanRules.Status.MISSING_PARENT) {
            add(I18n.get("tc.discoveryerror", I18n.get("tc.aspect.help." + packet.missingParent())), null, 0xffffff);
        }
        for (var reward : packet.rewards()) {
            var aspect = TCAspects.catalog().require(reward.aspect());
            if (reward.discovery()) {
                add("\u00a76" + net.minecraft.locale.Language.getInstance().getOrDefault("tc.addaspectdiscovery").replace("%n", thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()), aspect.tag(), thaumcraft.api.client.ClientAspectViews.view(aspect).color());
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.2f, 0.5f + mc.player.getRandom().nextFloat() * 0.2f);
            }
            if (reward.gainedAmount() <= 0) continue;
            // TC4's %n denotes an aspect name; it is not a Java printf newline.
            add(net.minecraft.locale.Language.getInstance().getOrDefault("tc.addaspectpool").replace("%s", Integer.toString(reward.gainedAmount())).replace("%n", thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()), aspect.tag(), thaumcraft.api.client.ClientAspectViews.view(aspect).color());
            long now = now();
            for (int i = 0; i < Math.min(64,reward.gainedAmount()); i++) {
                var random = mc.player.getRandom();
                long starts = now + random.nextInt(1000);
                TIMELINE.fly(aspect.tag(), 0.4f + random.nextFloat() * 0.2f, 0.4f + random.nextFloat() * 0.2f, starts);
            }
            if (now > nextPoolSound) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1f, 0.9f + mc.player.getRandom().nextFloat() * 0.2f);
                nextPoolSound = now + 100;
            }
        }
    }
    private static void add(String text, String aspect, int color) {
        TIMELINE.add(text, aspect, color, now(), TCConfig.CLIENT.notificationDelay.get());
    }
    public static void notice(String text) { add(text, null, 0xffffff); }
    private static long now() { return System.nanoTime() / 1_000_000; }
    public static void render(GuiGraphics graphics, int width, int height) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        long now = now();
        int delay = TCConfig.CLIENT.notificationDelay.get();
        var notices = TIMELINE.notices(now, delay);
        float shift = -8;
        for (int i = 0; i < notices.size() && i < TCConfig.CLIENT.notificationMax.get(); i++) {
            var notice = notices.get(i);
            int halfWidth = mc.font.width(notice.text()) / 2;
            int alpha = 255;
            if (i == notices.size() - 1 && notice.created() > now) alpha = 255 - (int) ((float) (notice.created() - now) / (delay / 4) * 240);
            if (notice.expires() < now + delay) {
                alpha = (int) (255 - (float) (now + delay - notice.expires()) / delay * 240);
                shift = -8 * (alpha / 255f);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(width - halfWidth - 10, height - i * 8 + shift, 0);
            graphics.pose().scale(0.5f, 0.5f, 0.5f);
            graphics.drawString(mc.font, notice.text(), -4, -8, (alpha / 2 << 24) | 0xffffff, true);
            graphics.pose().popPose();
            if (notice.aspect() != null) icon(graphics, aspectTexture(notice.aspect()), width - 9, height - i * 8 + shift - 6, 8, notice.color(), alpha / 511f);
            if (i == notices.size() - 1 && notice.created() > now) {
                float scale = (float) (notice.created() - now) / (delay / 4);
                int fade = 255 - (int) (scale * 240);
                graphics.pose().pushPose();
                graphics.pose().translate(width - 5 - 8 * scale - Math.pow(1 - scale, 3) * halfWidth * 3,
                        height - i * 8 + shift - 2 - 8 * scale, 0);
                graphics.pose().scale(scale, scale, scale);
                RenderSystem.enableBlend();
                graphics.setColor(1, 1, 1, 0.5f - fade / 511f);
                LegacyGui.blit(graphics,PARTICLES, 0, 0, 16 * ((mc.player.tickCount + i * 3) % 16), 80, 16, 16);
                graphics.setColor(1, 1, 1, 1);
                graphics.pose().popPose();
            }
        }
        float bookAlpha = 0;
        for (var flight : TIMELINE.flights(now)) {
            if (flight.created() > now) continue;
            double t = (now - flight.created()) / (double) (flight.expires() - flight.created());
            float alpha = t < 0.3f ? (float) (t / 0.3f) : t > 0.66f ? (float) (1 - (t - 0.66f) / 0.34f) : 1;
            bookAlpha = Math.max(bookAlpha, alpha);
            double x = Math.pow(1 - t, 2) * (int) (width * flight.x()) + 2 * (1 - t) * t * (int) (width * (0.25f + flight.x())) + t * t * width;
            double y = Math.pow(1 - t, 2) * (int) (height * flight.y()) + 2 * (1 - t) * t * (int) (height * flight.y()) - t * t * 8;
            icon(graphics, aspectTexture(flight.aspect()), x, y, 19.2f * alpha, TCAspects.catalog().require(flight.aspect()).color(), alpha * 0.66f);
        }
        if (bookAlpha > 0) icon(graphics, BOOK, width - 16, 0, 16, 0xffffff, bookAlpha);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
    private static ResourceLocation aspectTexture(String aspect) { return thaumcraft.api.client.ClientAspectViews.texture(aspect); }
    private static void icon(GuiGraphics graphics, ResourceLocation texture, double x, double y, float size, int color, float alpha) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size / 16, size / 16, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
        LegacyGui.blit(graphics,texture, 0, 0, 0, 0, 16, 16, 16, 16);
        graphics.setColor(1, 1, 1, 1);
        graphics.pose().popPose();
    }
}
