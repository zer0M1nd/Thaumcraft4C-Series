package thaumicomniverse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import thaumicomniverse.compat.backpack.ArcaneCraftingContainer;

public final class ArcaneCraftingTab extends UpgradeSettingsTab<ArcaneCraftingContainer> {
    public ArcaneCraftingTab(ArcaneCraftingContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("item.thaumicomniverse.arcane_crafting_upgrade"),
                Component.translatable("item.thaumicomniverse.arcane_crafting_upgrade"));
        openTabDimension = new Dimension(116, 202);
        toggle("intoStorage", 3, new UV(0, 0), new UV(16, 0));
        toggle("refill", 23, new UV(208, 0), new UV(224, 0));
        action("rotate", 43, new UV(64, 0), () -> container.rotate(!net.minecraft.client.gui.screens.Screen.hasShiftDown()));
        action("clear", 63, new UV(32, 0), container::clearGrid);
    }
    private void toggle(String key, int offset, UV on, UV off) {
        var definition = ButtonDefinitions.createToggleButtonDefinition(ButtonDefinitions.getBooleanStateData(
                GuiHelper.getButtonStateData(on, "omniverse.arcane." + key + ".on", Dimension.SQUARE_16, new Position(1, 1)),
                GuiHelper.getButtonStateData(off, "omniverse.arcane." + key + ".off", Dimension.SQUARE_16, new Position(1, 1))));
        addHideableChild(new ToggleButton<>(new Position(x + offset, y + 25), definition,
                button -> getContainer().setting(key, !getContainer().setting(key)), () -> getContainer().setting(key)));
    }
    private void action(String key, int offset, UV icon, Runnable run) {
        var state = GuiHelper.getButtonStateData(icon, "omniverse.arcane." + key, Dimension.SQUARE_16, new Position(1, 1));
        var definition = ButtonDefinitions.createToggleButtonDefinition(ButtonDefinitions.getBooleanStateData(state, state));
        addHideableChild(new ToggleButton<>(new Position(x + offset, y + 25), definition, button -> run.run(), () -> false));
    }
    @Override protected void moveSlotsToTab() {
        var slots = getContainer().getSlots();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            int localX = i == 0 || i == 1 ? 85 : 7 + (i - 2) % 3 * 18;
            int localY = i == 0 ? 90 : i == 1 ? 54 : 54 + (i - 2) / 3 * 18;
            thaumicomniverse.compat.backpack.SlotPosition.move(slot,
                    x - screen.getGuiLeft() + localX, y - screen.getGuiTop() + localY);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (!getContainer().isOpen()) return;
        getContainer().refresh();
        GuiHelper.renderSlotsBackground(graphics, x + 6, y + 53, 3, 3);
        GuiHelper.renderSlotsBackground(graphics, x + 84, y + 53, 1, 1);
        GuiHelper.renderSlotsBackground(graphics, x + 84, y + 89, 1, 1);
        graphics.drawString(minecraft.font, ">", x + 67, y + 92, 0xff000000, false);
        int index = 0;
        for (var entry : getContainer().visCost().entrySet()) {
            int left = x + 6 + index % 2 * 54, top = y + 121 + index / 2 * 24;
            tc4revival.client.AspectTooltips.draw(graphics,minecraft.font,new tc4revival.client.AspectTooltips.Cell(entry.getKey().tag(),""),left,top);
            var stack = getContainer().wand();
            var wand = stack.getItem() instanceof tc4revival.content.CastingWandItem value ? value : null;
            float cost = entry.getValue() * (wand == null ? 1 : wand.consumptionModifier(stack, minecraft.player, entry.getKey(), true));
            boolean enough = wand != null && wand.vis(stack, entry.getKey()) >= cost * 100;
            graphics.drawString(minecraft.font, new java.text.DecimalFormat("0.##").format(cost),
                    left + 18, top + 4, enough ? 0xff000000 : 0xffa00000, false);
            index++;
        }
    }
}
