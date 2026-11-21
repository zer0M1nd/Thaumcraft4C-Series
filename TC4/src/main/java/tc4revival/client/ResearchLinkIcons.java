package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchDisplayItems;

/** Read-only mirrors used by tutorial recommendations. Hidden research keeps its unknown icon. */
public final class ResearchLinkIcons {
    private ResearchLinkIcons() {}
    public static void draw(GuiGraphics g, String key, int x, int y, int size) {
        var entry = ResearchCatalogs.bundled().find(key).orElse(null);
        if (entry == null) return;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(size / 16f, size / 16f, 1);
            if (BookComponents.hidden(key)) {
                g.drawCenteredString(Minecraft.getInstance().font, "?", 8, 4, 0xffb5aa92);
            } else {
                g.setColor(1, 1, 1, .65f);
                if (entry.display().iconItem().isPresent()) {
                    ResearchDisplayItems.bundled().resolve(entry.display().iconItem().orElseThrow(), System.currentTimeMillis())
                            .ifPresent(stack -> ResearchMirrorItems.draw(g, stack, 0, 0));
                } else entry.display().iconResource().ifPresent(texture ->
                        LegacyGui.blit(g, ResourceLocation.parse(texture), 0, 0, 0, 0, 16, 16, 16, 16));
            }
        } finally { g.setColor(1, 1, 1, 1); g.pose().popPose(); }
    }
}
