package tbmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tbmodern.Overchanter;

public final class OverchanterScreen extends AbstractContainerScreen<Overchanter.Menu> {
    public OverchanterScreen(Overchanter.Menu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        var texture=net.minecraft.resources.ResourceLocation.parse("minecraft:textures/gui/container/dispenser.png");
        tc4revival.client.LegacyGui.blit(graphics,texture,leftPos,topPos,0,0,imageWidth,imageHeight);
        // DummyCore GuiCommon removes the dispenser grid, then draws actual container slots.
        tc4revival.client.LegacyGui.blit(graphics,texture,leftPos+60,topPos+16,6,6,28,54);
        tc4revival.client.LegacyGui.blit(graphics,texture,leftPos+88,topPos+16,6,6,28,54);
        for(var slot:menu.slots)tc4revival.client.LegacyGui.blit(graphics,texture,leftPos+slot.x-1,topPos+slot.y-1,7,83,18,18);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY);
    }
}
