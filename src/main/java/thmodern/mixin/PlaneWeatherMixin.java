package thmodern.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thmodern.plane.PocketPlanes;

/** Preserve the legacy provider's no-natural-freeze/snow policy, including cold plane biomes. */
@Mixin(value=Biome.class,remap=false)
public abstract class PlaneWeatherMixin {
    @Inject(method={"shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z","m_47480_"},at=@At("HEAD"),cancellable=true)
    private void th$noFreeze(LevelReader reader,BlockPos pos,boolean edge,CallbackInfoReturnable<Boolean> result) {
        if(reader instanceof Level level&&level.dimension().location().equals(PocketPlanes.DIMENSION))result.setReturnValue(false);
    }
    @Inject(method={"shouldSnow","m_47519_"},at=@At("HEAD"),cancellable=true)
    private void th$noSnow(LevelReader reader,BlockPos pos,CallbackInfoReturnable<Boolean> result) {
        if(reader instanceof Level level&&level.dimension().location().equals(PocketPlanes.DIMENSION))result.setReturnValue(false);
    }
}
