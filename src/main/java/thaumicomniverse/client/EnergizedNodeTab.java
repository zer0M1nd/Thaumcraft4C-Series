package thaumicomniverse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import tc4revival.aspect.TCAspects;
import thaumicomniverse.compat.backpack.EnergizedNodeContainer;

public final class EnergizedNodeTab extends UpgradeSettingsTab<EnergizedNodeContainer> {
    public EnergizedNodeTab(EnergizedNodeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("item.thaumicomniverse.energized_node_upgrade"),
                Component.translatable("item.thaumicomniverse.energized_node_upgrade.tooltip"));
        openTabDimension = new Dimension(110, 120);
    }
    @Override protected void moveSlotsToTab() {}
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        int index = 0; var rates = getContainer().rates();
        for (var aspect : thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.LEGACY_SIX)) {
            int left = x + 6 + index % 2 * 52, top = y + 29 + index / 2 * 24;
            graphics.blit(aspect.texture(), left, top, 0, 0, 16, 16, 16, 16);
            graphics.drawString(minecraft.font, Integer.toString(rates.amount(aspect)), left + 19, top + 4, 0xffd9d3dd, false);
            index++;
        }
        graphics.drawString(minecraft.font, "cv/t", x + 6, y + 102, 0xffd9d3dd, false);
    }
}
