package thaumicomniverse.mixin;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep the empty egg position for entities such as players and illusioners. */
@Pseudo
@Mixin(targets={"slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory",
        "slimeknights.tconstruct.plugin.jei.entity.SeveringCategory"},remap=false)
public abstract class TconstructEntityJeiMixin {
    @Redirect(method="setRecipe",at=@At(value="INVOKE",target="Lmezz/jei/api/gui/builder/IIngredientAcceptor;addItemStacks(Ljava/util/List;)Lmezz/jei/api/gui/builder/IIngredientAcceptor;"),remap=false,require=0)
    private IIngredientAcceptor<?> omniverse$preserveEmptyEggs(IIngredientAcceptor<?> acceptor,List<ItemStack> eggs) {
        return omniverse$preserveIngredientSlots(acceptor,VanillaTypes.ITEM_STACK,eggs);
    }

    // TConstruct 3.12 uses the typed API; JEI 15.74 removed the old item shortcuts.
    // Both sites are optional because supported TConstruct versions use different calls.
    @Redirect(method="setRecipe",at=@At(value="INVOKE",target="Lmezz/jei/api/gui/builder/IIngredientAcceptor;addIngredients(Lmezz/jei/api/ingredients/IIngredientType;Ljava/util/List;)Lmezz/jei/api/gui/builder/IIngredientAcceptor;"),remap=false,require=0)
    private <T> IIngredientAcceptor<?> omniverse$preserveIngredientSlots(IIngredientAcceptor<?> acceptor,IIngredientType<T> type,List<T> ingredients) {
        if (type != VanillaTypes.ITEM_STACK) return acceptor.addIngredients(type,ingredients);
        for (T ingredient:ingredients) {
            if (ingredient == null || ((ItemStack)ingredient).isEmpty()) acceptor.addOptionalTypedIngredients(List.of(Optional.empty()));
            else acceptor.addIngredient(type,ingredient);
        }
        return acceptor;
    }
}
