package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.FocusPouchMenu;

public  class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/gui/gui_focuspouch.png");
    public FocusPouchScreen(FocusPouchMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 175; imageHeight = 232; }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int slot = menu.lockedHotbarSlot(); if (slot < 0) return;
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
        LegacyGui.blit(graphics,TEXTURE, 8 + slot * 18, 209, 240, 0, 16, 16); graphics.pose().popPose();
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) { LegacyGui.blit(graphics,TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) { return false; }
}
