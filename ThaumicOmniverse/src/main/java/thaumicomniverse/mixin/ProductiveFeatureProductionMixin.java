package thaumicomniverse.mixin;

import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="cy.jdkdigital.productivebees.util.BeeHelper",remap=false)
public abstract class ProductiveFeatureProductionMixin {
    @Unique private static boolean omniverse$disabled(Entity entity) {
        return entity instanceof ConfigurableBee bee && bee.getBeeType().startsWith("thaumicomniverse:")
                && !thaumicomniverse.RevivalFeatures.Group.PRODUCTIVE_BEES.enabled();
    }
    @Inject(method="getBeeProduce",at=@At("HEAD"),cancellable=true,remap=false)
    private static void omniverse$production(Level level,Bee bee,boolean simulate,double multiplier,CallbackInfoReturnable<List<ItemStack>> ci) {
        if(omniverse$disabled(bee)) ci.setReturnValue(List.of());
    }
    @Inject(method="getBreedingResult",at=@At("RETURN"),cancellable=true,remap=false)
    private static void omniverse$breeding(Bee bee,AgeableMob partner,ServerLevel level,CallbackInfoReturnable<Entity> ci) {
        if(omniverse$disabled(bee)||omniverse$disabled(partner)||omniverse$disabled(ci.getReturnValue()))ci.setReturnValue(null);
    }
    @Inject(method="getBreedingRecipes(Lcy/jdkdigital/productivebees/util/BeeHelper$IdentifierInventory;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/List;",at=@At("RETURN"),cancellable=true,remap=false)
    private static void omniverse$machineRecipes(cy.jdkdigital.productivebees.util.BeeHelper.IdentifierInventory inventory,
            ServerLevel level,CallbackInfoReturnable<List<cy.jdkdigital.productivebees.common.recipe.BeeBreedingRecipe>> ci) {
        if(thaumicomniverse.RevivalFeatures.Group.PRODUCTIVE_BEES.enabled())return;
        if(thaumicomniverse.compat.ProductiveFeatureRules.disabled(inventory.getIdentifier(0))
                ||thaumicomniverse.compat.ProductiveFeatureRules.disabled(inventory.getIdentifier(1)))ci.setReturnValue(List.of());
        else ci.setReturnValue(ci.getReturnValue().stream().filter(recipe->recipe.offspring==null||recipe.offspring.get()==null
                ||!thaumicomniverse.compat.ProductiveFeatureRules.disabled(recipe.offspring.get().getBeeType().toString())).toList());
    }
}
