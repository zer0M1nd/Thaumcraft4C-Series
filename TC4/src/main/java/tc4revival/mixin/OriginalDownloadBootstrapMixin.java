package tc4revival.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.client.loading.ClientModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Must precede all addon pack finders, including Thaumic Bases' independent loader. */
@Mixin(value = ClientModLoader.class, remap = false)
public abstract class OriginalDownloadBootstrapMixin {
    @Inject(method = "begin", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/ModLoader;postEvent(Lnet/minecraftforge/eventbus/api/Event;)V"))
    private static void tc4$downloadOriginals(Minecraft minecraft, PackRepository packs, ReloadableResourceManager resources, CallbackInfo callback) {
        tc4revival.resources.OriginalDownloadBootstrap.beforeResourceFinders();
    }
}
