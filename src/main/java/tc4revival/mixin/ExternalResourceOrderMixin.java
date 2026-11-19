package tc4revival.mixin;

import java.util.Collection;
import java.util.List;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.resources.ExternalResourceOrder;

/** Client-only: also runs when selected resource packs are changed or reloaded. */
@Mixin(value = PackRepository.class, remap = false)
public abstract class ExternalResourceOrderMixin {
    @Inject(method = {"rebuildSelected", "m_10517_"}, at = @At("RETURN"), cancellable = true)
    private void tc4$resourceOrder(Collection<String> requested, CallbackInfoReturnable<List<Pack>> callback) {
        callback.setReturnValue(ExternalResourceOrder.normalize(callback.getReturnValue()));
    }
}
