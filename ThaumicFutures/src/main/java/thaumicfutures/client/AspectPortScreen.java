package thaumicfutures.client;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.Aspect;
import thaumicfutures.compat.machinery.AspectPort;
import thaumicfutures.compat.machinery.AspectPortMenu;

public final class AspectPortScreen extends AbstractContainerScreen<AspectPortMenu> {
    private final ArrayList<Button> sides = new ArrayList<>();
    private int page;
    public AspectPortScreen(AspectPortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 238;
    }
    @Override protected void init() {
        super.init();
        sides.clear();
        for (Direction direction : Direction.values()) {
            int ordinal = direction.ordinal();
            sides.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ordinal);
            }).bounds(leftPos + 8 + ordinal % 3 * 78, topPos + 24 + ordinal / 3 * 22, 76, 20).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1))
                .bounds(leftPos + 8, topPos + 213, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> page = Math.min(maxPage(), page + 1))
                .bounds(leftPos + 216, topPos + 213, 24, 18).build());
    }
    private int maxPage() { return Math.max(0, (menu.port.contents().size() - 1) / 7); }
    @Override protected void containerTick() {
        super.containerTick();
        page = Math.min(page, maxPage());
        for (Direction direction : Direction.values()) {
            boolean active = menu.port.kind() == AspectPort.Kind.BRIDGE ? menu.port.absorption() == direction : menu.port.enabled(direction);
            sides.get(direction.ordinal()).setMessage(Component.translatable("omniverse.machine.side",
                    Component.translatable("omniverse.machine.face." + direction.getName()),
                    Component.translatable(active ? "omniverse.machine.on" : "omniverse.machine.off")));
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff393638);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xffc6c6c6);
        graphics.fill(leftPos + 7, topPos + 77, leftPos + imageWidth - 7, topPos + 210, 0xff282528);
        var contents = menu.port.contents();
        var aspects = contents.aspects().stream().sorted(Comparator.comparing(Aspect::tag)).toList();
        for (int i = page * 7; i < Math.min(aspects.size(), page * 7 + 7); i++) {
            Aspect aspect = aspects.get(i);
            int y = topPos + 80 + (i % 7) * 18;
            graphics.blit(thaumcraft.api.aspects.AspectViews.display(aspect).texture(), leftPos + 10, y, 0, 0, 16, 16, 16, 16);
            String quantity = contents.amount(aspect) + " / " + menu.port.capacity();
            graphics.drawString(font, font.plainSubstrByWidth(thaumcraft.api.aspects.AspectViews.display(aspect).displayName(), imageWidth - 48 - font.width(quantity)),
                    leftPos + 30, y + 4, 0xffffff, false);
            graphics.drawString(font, quantity, leftPos + imageWidth - 12 - font.width(quantity), y + 4, 0xffffff, false);
        }
        if (aspects.isEmpty()) graphics.drawString(font, Component.translatable("omniverse.machine.empty"), leftPos + 12, topPos + 84, 0xffffff, false);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, font.substrByWidth(title, imageWidth - 16).getString(), 8, 8, 0x252025, false);
        graphics.drawCenteredString(font, Component.translatable(menu.port.kind().essentia()
                ? "omniverse.machine.essentia_units" : "omniverse.machine.vis_units"), imageWidth / 2, 67, 0x333333);
        graphics.drawCenteredString(font, (page + 1) + " / " + (maxPage() + 1), imageWidth / 2, 218, 0x333333);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
