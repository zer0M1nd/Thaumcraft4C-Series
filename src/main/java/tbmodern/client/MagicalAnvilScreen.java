package tbmodern.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

public final class MagicalAnvilScreen extends AnvilScreen {
    private final Inventory inventory;
    public MagicalAnvilScreen(AnvilMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); this.inventory = inventory; }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        int cost = menu.getCost();
        if (cost <= 0) return;
        boolean result = menu.getSlot(2).hasItem();
        Component text = !result && cost >= 60 && !inventory.player.getAbilities().instabuild
            ? Component.translatable("container.repair.expensive") : Component.translatable("container.repair.cost", cost);
        int x = imageWidth - 8 - font.width(text), color = result && (inventory.player.getAbilities().instabuild || inventory.player.experienceLevel >= cost) ? 0x80ff20 : 0xff6060;
        graphics.fill(x - 2, 67, imageWidth - 6, 79, 0xff404040);
        graphics.drawString(font, text, x, 69, color, false);
    }
}
