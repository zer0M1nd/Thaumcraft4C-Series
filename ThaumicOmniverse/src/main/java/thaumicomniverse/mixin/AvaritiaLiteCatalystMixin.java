package thaumicomniverse.mixin;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.bullfighter.avaritia.ExtremeCraftingTableRecipeShapeless",remap=false)
public abstract class AvaritiaLiteCatalystMixin {
    @Inject(method="execute",at=@At("HEAD"),cancellable=true,remap=false)
    private static void omniverse$catalyst(Entity player,List<ItemStack> required,ItemStack result,CallbackInfo ci){
        if(thaumicomniverse.compat.AvaritiaRecipes.craftLiteCatalyst(player,required,result))ci.cancel();
    }
}
