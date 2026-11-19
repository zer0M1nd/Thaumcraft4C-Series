package thaumicomniverse.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.expansion.AncientForest;

// A RETURN injection misses TerraBlender's synthetic cancellable HEAD return.
// Sample downstream with a recursion guard before that early return instead.
@Mixin(value=MultiNoiseBiomeSource.class,priority=900)
public abstract class AncientBiomeMixin {
    @Inject(method={"getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            "m_203407_(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"},
            at=@At("HEAD"),cancellable=true,remap=false)
    private void ancient(int x,int y,int z,Climate.Sampler sampler,CallbackInfoReturnable<Holder<Biome>> result){
        var selected=AncientForest.sample((BiomeSource)(Object)this,x,y,z,sampler);
        if(selected!=null)result.setReturnValue(selected);
    }
}
