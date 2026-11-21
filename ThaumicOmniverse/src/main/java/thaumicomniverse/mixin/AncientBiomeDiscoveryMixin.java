package thaumicomniverse.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeSource.class)
public abstract class AncientBiomeDiscoveryMixin {
    @Inject(method={"possibleBiomes","m_207840_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void discover(CallbackInfoReturnable<Set<Holder<Biome>>> result){
        result.setReturnValue(thaumicomniverse.expansion.AncientForest.possible(result.getReturnValue()));
    }
}
