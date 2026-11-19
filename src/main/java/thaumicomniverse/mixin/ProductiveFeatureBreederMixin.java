package thaumicomniverse.mixin;

import cy.jdkdigital.productivebees.common.block.entity.BreedingChamberBlockEntity;
import cy.jdkdigital.productivebees.common.recipe.BeeBreedingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.ProductiveFeatureRules;

@Mixin(value=BreedingChamberBlockEntity.class,remap=false)
public abstract class ProductiveFeatureBreederMixin {
    @Inject(method="canProcessInput",at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$input(IItemHandlerModifiable inventory,boolean firstRun,CallbackInfoReturnable<Boolean> ci) {
        var tile=(BreedingChamberBlockEntity)(Object)this;
        if(ProductiveFeatureRules.breedingDisabled(inventory,(BeeBreedingRecipe)tile.getCurrentRecipe()))ci.setReturnValue(false);
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void omniverse$pause(Level level,BlockPos pos,BlockState state,BreedingChamberBlockEntity tile,CallbackInfo ci) {
        if(tile.getCapability(ForgeCapabilities.ITEM_HANDLER).map(inventory->
                ProductiveFeatureRules.breedingDisabled(inventory,(BeeBreedingRecipe)tile.getCurrentRecipe())).orElse(false)) {
            tile.isRunning=false;ci.cancel();
        }
    }
}
