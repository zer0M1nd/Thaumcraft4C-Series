package thaumicomniverse.mixin;

import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.backpack.ArcaneCraftingContainer;

@Mixin(value = StorageContainerMenuBase.class, remap = false)
public abstract class BackpackArcaneRefreshMixin {
    @Inject(method = {"broadcastChanges()V", "m_38946_()V"}, at = @At("HEAD"), require = 1)
    private void omniverse$refresh(CallbackInfo ci) {
        omniverse$refreshAll();
    }
    @Inject(method = {"clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
            "m_150399_(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"}, at = @At("HEAD"), require = 1)
    private void omniverse$beforeClick(CallbackInfo ci) {
        omniverse$refreshAll();
    }
    @org.spongepowered.asm.mixin.Unique
    private void omniverse$refreshAll() {
        ((StorageContainerMenuBase<?>) (Object) this).getUpgradeContainers().values().forEach(container -> {
            if (container instanceof ArcaneCraftingContainer arcane) arcane.refresh();
        });
    }
}
