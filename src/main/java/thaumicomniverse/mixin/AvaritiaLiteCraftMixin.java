package thaumicomniverse.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.bullfighter.avaritia.procedures.ExtremeCraftingTableRecipesProcedure",remap=false)
public abstract class AvaritiaLiteCraftMixin {
    @Inject(method="execute",at=@At("TAIL"),remap=false)
    private static void omniverse$craftWand(Entity player,CallbackInfo ci){
        thaumicomniverse.compat.AvaritiaRecipes.craftLiteWand(player);
    }
}
