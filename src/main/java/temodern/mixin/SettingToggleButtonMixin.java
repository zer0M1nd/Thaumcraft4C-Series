package temodern.mixin;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.TypeFilter;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.ButtonToolTips;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.TCAspects;
import temodern.client.EssentiaRendering;

@Mixin(value = SettingToggleButton.class, remap = false)
public abstract class SettingToggleButtonMixin extends IconButton {
    @Shadow @Final private Setting<?> buttonSetting;

    @Shadow private Enum<?> currentValue;

    protected SettingToggleButtonMixin(OnPress onPress) {
        super(onPress);
    }

    @Shadow
    private static <T extends Enum<T>> void registerApp(Icon icon, Setting<T> setting, T value,
            ButtonToolTips title, Component... tooltip) {
        throw new AssertionError();
    }

    @Inject(method = "<init>(Lappeng/api/config/Setting;Ljava/lang/Enum;Ljava/util/function/Predicate;Lappeng/client/gui/widgets/SettingToggleButton$IHandler;)V",
            at = @At("RETURN"))
    private void thaumicenergistics$registerEssentiaAppearance(CallbackInfo ci) {
        if (buttonSetting == Settings.TYPE_FILTER) {
            registerApp(Icon.TOOLBAR_BUTTON_BACKGROUND, Settings.TYPE_FILTER,
                    TypeFilter.valueOf("THAUMICENERGISTICS_ESSENTIA"), ButtonToolTips.TypeFilter,
                    Component.translatable("gui.thaumicenergistics.show_essentia"));
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (visible && buttonSetting == Settings.TYPE_FILTER && currentValue == TypeFilter.valueOf("THAUMICENERGISTICS_ESSENTIA")) {
            EssentiaRendering.drawAspect(graphics, getX(), getY(), isHalfSize() ? 8 : 16, TCAspects.get(LegacyAspect.MAGIC));
        }
    }
}
