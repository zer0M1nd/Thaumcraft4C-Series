package thaumicfutures.mixin;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.genetics.IIndividual;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="forestry.apiculture.genetics.Bee",remap=false)
public abstract class ForestryFeatureProductionMixin {
    @Inject(method="produceStacks",at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$products(IBeeHousing housing,CallbackInfoReturnable<List<ItemStack>> ci) {
        if(thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return;
        var bee=(IIndividual)this;
        if(bee.getSpecies().id().getNamespace().equals("thaumicfutures")
                || bee.getInactiveSpecies().id().getNamespace().equals("thaumicfutures")) {
            ci.setReturnValue(List.of());return;
        }
        // Mo'Bees species keep their own production; remove only this integration's added products.
        ci.setReturnValue(ci.getReturnValue().stream().filter(stack->{
            var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id==null||!id.getNamespace().equals("thaumicfutures");
        }).toList());
    }
}
