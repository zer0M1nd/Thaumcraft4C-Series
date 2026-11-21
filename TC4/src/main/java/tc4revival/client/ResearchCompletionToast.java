package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tc4revival.network.ResearchCompletionPacket;

/** Research notifications use Minecraft's toast queue, animation and background. */
 class ResearchCompletionToast implements Toast {
    private final ResearchCompletionPacket packet;
    private final Connection source;
    ResearchCompletionToast(ResearchCompletionPacket packet, Connection source) {
        this.packet = packet; this.source = source;
    }
    @Override public Visibility render(GuiGraphics graphics, ToastComponent toasts, long elapsed) {
        var mc = toasts.getMinecraft();
        if (mc.player == null || mc.getConnection() == null
                || mc.getConnection().getConnection() != source || !source.isConnected()) return Visibility.HIDE;
        graphics.blit(TEXTURE, 0, 0, 0, 0, width(), height());
        var font = mc.font;
        var title = Component.translatable("tc.research_name." + packet.key());
        var lines = font.split(title, 125);
        double multiplier = Math.max(1, toasts.getNotificationDisplayTimeMultiplier());
        long time = (long) (elapsed / multiplier);
        // Keep long research titles readable with the modern font, rather than shrinking them.
        if (lines.size() <= 1 || time < 1000) {
            graphics.drawString(font, Component.translatableWithFallback("thaumcraft.toast.research_complete", "Research completed!"), 30, 7, 0xffff00, false);
            if (lines.size() == 1) graphics.drawString(font, lines.get(0), 30, 18, 0xffffff, false);
        } else {
            int first = Math.min(lines.size() - 1, (int) ((time - 1000) / 1500) * 2);
            graphics.drawString(font, lines.get(first), 30, 7, 0xffffff, false);
            if (first + 1 < lines.size()) graphics.drawString(font, lines.get(first + 1), 30, 18, 0xffffff, false);
        }
        if (!packet.icon().isEmpty()) graphics.renderItem(packet.icon(), 8, 8);
        else if (!packet.texture().isEmpty()) LegacyGui.blit(graphics, ResourceLocation.parse(packet.texture()), 8, 8, 0, 0, 16, 16, 16, 16);
        long duration = Math.max(5000, 1000L + (lines.size() + 1) / 2 * 1500L);
        return time < duration ? Visibility.SHOW : Visibility.HIDE;
    }
}
