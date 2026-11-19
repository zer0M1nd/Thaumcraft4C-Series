package thaumicomniverse.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.expansion.AncientForest;

/** Preserve the authored legacy blue palette while retaining Quark's tint elsewhere. */
@Pseudo
@Mixin(targets="org.violetmoon.quark.content.client.module.GreenerGrassModule$Client",remap=false)
public abstract class QuarkAncientForestColorsMixin {
    @Inject(method="getConvulsedColor",at=@At("RETURN"),cancellable=true)
    private void omniverse$legacyPalette(BlockColor original,BooleanSupplier condition,CallbackInfoReturnable<BlockColor> cir){
        var greener=cir.getReturnValue();
        cir.setReturnValue((state,world,pos,tint)->{
            var level=Minecraft.getInstance().level;
            return world!=null&&pos!=null&&level!=null&&level.getBiome(pos).is(AncientForest.BIOME)
                    ?original.getColor(state,world,pos,tint):greener.getColor(state,world,pos,tint);
        });
    }
}
