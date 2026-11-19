package thaumicomniverse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import thaumicomniverse.compat.backpack.EssentiaTankContainer;

public final class EssentiaTankTab extends UpgradeSettingsTab<EssentiaTankContainer> {
    public EssentiaTankTab(EssentiaTankContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("item.thaumicomniverse.essentia_tank_upgrade"),
                Component.translatable("item.thaumicomniverse.essentia_tank_upgrade"));
        openTabDimension = new Dimension(80, 95);
    }
    @Override protected void moveSlotsToTab() {}
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        int amount = getContainer().amount(), capacity = getContainer().capacity(); var aspect = getContainer().aspect();
        graphics.fill(x + 8, y + 27, x + 28, y + 77, 0xff25202e);
        graphics.fill(x + 10, y + 29, x + 26, y + 75, 0xff080c12);
        if (aspect != null && amount > 0) {
            graphics.fill(x + 10, y + 75 - (int)(Math.min(amount,capacity) * 46L / Math.max(1,capacity)), x + 26, y + 75, 0xff000000 | aspect.color());
            tc4revival.client.AspectTooltips.draw(graphics,minecraft.font,new tc4revival.client.AspectTooltips.Cell(aspect.tag(),""),x+39,y+30);
        }
        graphics.drawString(minecraft.font, amount + "/" + capacity, x + 33, y + 54, 0xff000000, false);
    }
}
