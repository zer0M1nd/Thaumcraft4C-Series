package thaumicomniverse.mixin;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProduct;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GTOreByProduct.class,remap=false)
public abstract class GregTechOreDiagramMixin {
    @Shadow @Final protected List<ItemEntryList> itemInputs;
    @Shadow @Final protected NonNullList<ItemStack> itemOutputs;
    @Shadow @Final protected List<FluidEntryList> fluidInputs;
    @Shadow @Final private Int2ObjectMap<Content> chances;
    @Shadow private boolean hasChemBath;
    @Inject(method="<init>",at=@At("RETURN"))
    private void omniverse$actualRecipes(Material material,CallbackInfo ci) {
        var level=net.minecraft.client.Minecraft.getInstance().level;
        if(level!=null&&thaumicomniverse.compat.gregtech.ArcaneOreDiagram.apply(material,level.getRecipeManager(),itemInputs,itemOutputs,fluidInputs,chances))hasChemBath=true;
    }
}
