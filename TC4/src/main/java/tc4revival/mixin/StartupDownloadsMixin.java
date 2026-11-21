package tc4revival.mixin;

import com.mojang.realmsclient.client.RealmsClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tc4revival.client.OriginalResourceScreen;

/** Gate both the normal initial screen and Quick Play before either starts. */
@Mixin(value = Minecraft.class, remap = false)
public abstract class StartupDownloadsMixin {
    @Shadow(aliases = {"m_278684_"})
    private void setInitialScreen(
            RealmsClient realms, ReloadInstance reload, GameConfig.QuickPlayData quickPlay) {
        throw new AssertionError();
    }

    @Inject(
            method = {"setInitialScreen", "m_278684_"},
            at = @At("HEAD"),
            cancellable = true)
    private void tc4$waitForDownloads(
            RealmsClient realms,
            ReloadInstance reload,
            GameConfig.QuickPlayData quickPlay,
            CallbackInfo callback) {
        if (OriginalResourceScreen.beforeInitialScreen(
                () -> setInitialScreen(realms, reload, quickPlay))) callback.cancel();
    }
}
