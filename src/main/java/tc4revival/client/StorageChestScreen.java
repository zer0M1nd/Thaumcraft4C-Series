package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.StorageChestMenu;

public class StorageChestScreen extends AbstractContainerScreen<StorageChestMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");
    private Button previous, next;

    public StorageChestScreen(StorageChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override protected void init() {
        super.init();
        if (menu.pages() > 1) {
            previous = addRenderableWidget(Button.builder(Component.literal("‹"), b -> turn(-1))
                    .bounds(leftPos + 113, topPos + 125, 16, 13).build());
            next = addRenderableWidget(Button.builder(Component.literal("›"), b -> turn(1))
                    .bounds(leftPos + 152, topPos + 125, 16, 13).build());
            turn(0);
        }
    }
    private void turn(int delta) {
        menu.showPage(menu.page() + delta);
        if (delta != 0 && minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.page());
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pages();
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        super.renderLabels(g, x, y);
        if (menu.pages() > 1) g.drawCenteredString(font, (menu.page() + 1) + "/" + menu.pages(), 141, 128, 0x404040);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int x, int y) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 125);
        g.blit(TEXTURE, leftPos, topPos + 125, 0, 126, imageWidth, 96);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        renderBackground(g);
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
    }
}
