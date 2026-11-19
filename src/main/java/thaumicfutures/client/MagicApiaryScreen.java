package thaumicfutures.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumicfutures.compat.bees.MagicApiaryMenu;

public final class MagicApiaryScreen extends AbstractContainerScreen<MagicApiaryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumicfutures:textures/gui/magic_apiary.png");
    public MagicApiaryScreen(MagicApiaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelX = 9;
        inventoryLabelY = 99;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int filled = Math.max(0, Math.min(46, menu.progress * 46 / 100));
        int color = 176 + Math.min(4, filled * 5 / 46) * 2;
        graphics.blit(TEXTURE, leftPos + 21, topPos + 37 + 46 - filled, color, 46 - filled, 2, filled);
        if ((menu.boosts & 1) != 0) graphics.blit(TEXTURE, leftPos + 15, topPos + 17, 176, 71, 9, 13);
        if ((menu.boosts & 2) != 0) graphics.blit(TEXTURE, leftPos + 25, topPos + 17, 176, 58, 11, 13);
        if ((menu.boosts & 4) != 0) graphics.blit(TEXTURE, leftPos + 37, topPos + 18, 176, 46, 16, 12);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(15, 17, 39, 14, mouseX, mouseY))
            graphics.renderTooltip(font, Component.translatable("omniverse.bees.apiary_boosts",
                    (menu.boosts & 1) != 0 ? 2 : 1, (menu.boosts & 2) != 0 ? 2 : 1,
                    (menu.boosts & 4) != 0 ? 1.8 : .9), mouseX, mouseY);
    }
}
