package temodern.mixin;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.TypeFilter;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.ButtonToolTips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import temodern.TE;

@Mixin(value = SettingToggleButton.class, remap = false)
public abstract class SettingToggleButtonMixin {
    @Shadow @Final private Setting<?> buttonSetting;

    @Shadow
    private static <T extends Enum<T>> void registerApp(ItemLike icon, Setting<T> setting, T value,
            ButtonToolTips title, Component... tooltip) {
        throw new AssertionError();
    }

    @Inject(method = "<init>(Lappeng/api/config/Setting;Ljava/lang/Enum;Ljava/util/function/Predicate;Lappeng/client/gui/widgets/SettingToggleButton$IHandler;)V",
            at = @At("RETURN"))
    private void thaumicenergistics$registerEssentiaAppearance(CallbackInfo ci) {
        if (buttonSetting == Settings.TYPE_FILTER) {
            registerApp(TE.AMPOULE.get(), Settings.TYPE_FILTER,
                    TypeFilter.valueOf("THAUMICENERGISTICS_ESSENTIA"), ButtonToolTips.TypeFilter,
                    Component.translatable("gui.thaumicenergistics.show_essentia"));
        }
    }
}
