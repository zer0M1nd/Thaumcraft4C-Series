package thaumicomniverse.mixin;

import com.simibubi.create.compat.jei.category.SequencedAssemblyCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.*;
import java.util.List;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.*;

@Mixin(value=SequencedAssemblyCategory.class,remap=false)
public abstract class CreateAssemblyTooltipMixin {
    @Shadow private SequencedAssemblySubCategory getSubCategory(SequencedRecipe<?> recipe){throw new AssertionError();}
    @Inject(method="getTooltipStrings(Lcom/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;DD)Ljava/util/List;",at=@At("HEAD"),cancellable=true)
    private void omniverse$tooltip(SequencedAssemblyRecipe recipe,IRecipeSlotsView slots,double mouseX,double mouseY,CallbackInfoReturnable<List<Component>> cir){
        if(mouseY<=5 || mouseY>=84)return;
        int width=-3;for(var step:recipe.getSequence())width+=getSubCategory(step).getWidth()+3;
        double x=mouseX+width/2-((SequencedAssemblyCategory)(Object)this).getBackground().getWidth()/2;
        int index=0;
        for(var row:recipe.getSequence()){
            int w=getSubCategory(row).getWidth();
            if(x>=0 && x<w && row.getRecipe() instanceof ArcaneProcessingRecipe step){
                if(mouseY>=14&&mouseY<33&&x>=4&&x<22&&!step.spouting())
                    cir.setReturnValue(ArcaneAssemblyTooltips.lines(step,index+1));
                // The native category supplies the standard two-line machine tooltip.
                return;
            }
            x-=w+3;index++;
        }
    }
}
