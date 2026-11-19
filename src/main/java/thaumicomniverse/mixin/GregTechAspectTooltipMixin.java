package thaumicomniverse.mixin;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tc4revival.compat.jei.ThaumcraftJei;

/** Preserve TC's aspect tooltip through LDLib's custom JEI slot renderer. */
@Mixin(value=ModularUIRecipeCategory.class,remap=false)
public abstract class GregTechAspectTooltipMixin {
    @Redirect(method="addJEISlot",at=@At(value="INVOKE",target="Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;setCustomRenderer(Lmezz/jei/api/ingredients/IIngredientType;Lmezz/jei/api/ingredients/IIngredientRenderer;)Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;"),remap=false)
    @SuppressWarnings({"rawtypes","unchecked"})
    private static IRecipeSlotBuilder omniverse$aspectTooltip(IRecipeSlotBuilder builder,IIngredientType type,IIngredientRenderer renderer) {
        if(type==ThaumcraftJei.ASPECTS) {
            renderer=new ThaumcraftJei.AspectRenderer() {
                // The native widget already draws the icon and its amount.
                @Override public void render(GuiGraphics graphics,ThaumcraftJei.AspectValue value) {}
            };
        }
        return builder.setCustomRenderer(type,renderer);
    }
}
