package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.HoverHarnessMenu;

public  class HoverHarnessScreen extends AbstractContainerScreen<HoverHarnessMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/gui/guihoverharness.png");
    public HoverHarnessScreen(HoverHarnessMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        int slot = menu.lockedHotbarSlot(); if (slot < 0) return;
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
        LegacyGui.blit(graphics,TEXTURE, 8 + slot * 18, 142, 240, 0, 16, 16); graphics.pose().popPose();
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) { LegacyGui.blit(graphics,TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight); }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) { renderBackground(graphics); super.render(graphics, x, y, partial); renderTooltip(graphics, x, y); }
    @Override protected boolean checkHotbarKeyPressed(int key, int scan) { return false; }
}
