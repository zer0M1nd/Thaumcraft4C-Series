package thaumicomniverse.mixin;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = UpgradeWrapperBase.class, remap = false)
public interface BackpackWrapperAccess {
    @Accessor("storageWrapper") IStorageWrapper omniverse$storage();
}
