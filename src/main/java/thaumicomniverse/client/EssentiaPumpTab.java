package thaumicomniverse.client;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import thaumicomniverse.compat.backpack.EssentiaPumpContainer;

public final class EssentiaPumpTab extends UpgradeSettingsTab<EssentiaPumpContainer> {
    public EssentiaPumpTab(EssentiaPumpContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, Component.translatable("item.thaumicomniverse.essentia_pump_upgrade"),
                Component.translatable("item.thaumicomniverse.essentia_pump_upgrade"));
        openTabDimension = new Dimension(66, 68);
        button("input", 3, new UV(144, 0), new UV(160, 0), "pump_input", "pump_output");
        button("adjacent", 23, new UV(0, 112), new UV(16, 112), "interact_with_tanks_and_pipes", "do_not_interact_with_tanks_and_pipes");
        button("hand", 43, new UV(208, 0), new UV(224, 0), "interact_with_hand", "do_not_interact_with_hand");
    }
    private void button(String key, int offset, UV on, UV off, String onText, String offText) {
        var definition = ButtonDefinitions.createToggleButtonDefinition(ButtonDefinitions.getBooleanStateData(
                GuiHelper.getButtonStateData(on, TranslationHelper.INSTANCE.translUpgradeButton(onText), Dimension.SQUARE_16, new Position(1, 1)),
                GuiHelper.getButtonStateData(off, TranslationHelper.INSTANCE.translUpgradeButton(offText), Dimension.SQUARE_16, new Position(1, 1))));
        addHideableChild(new ToggleButton<>(new Position(x + offset, y + 25), definition,
                button -> getContainer().setting(key, !getContainer().setting(key)), () -> getContainer().setting(key)));
    }
    @Override protected void moveSlotsToTab() {}
    @Override protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY) {
        super.renderBg(graphics, minecraft, mouseX, mouseY);
        if (getContainer().isOpen() && getContainer().pending() > 0)
            graphics.drawString(minecraft.font, Component.translatable("omniverse.pump.pending", getContainer().pending()), x + 4, y + 48, 0xff000000, false);
    }
}
