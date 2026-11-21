package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.crafter.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.ArcaneMechanicalCrafting;

@Mixin(value=MechanicalCrafterBlockEntity.class,remap=false)
public abstract class CreateArcaneCrafterMixin {
    @Shadow protected RecipeGridHandler.GroupedItems groupedItems;
    @Inject(method="tick",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"),cancellable=true)
    private void omniverse$waitForVis(CallbackInfo ci){
        var root=(MechanicalCrafterBlockEntity)(Object)this;
        if(RecipeGridHandler.tryToApplyRecipe(root.getLevel(),groupedItems)!=null)return;
        var plan=ArcaneMechanicalCrafting.find(root,groupedItems);
        if(plan!=null&&plan.result(false).isEmpty())ci.cancel();
    }
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack omniverse$arcane(Level level,RecipeGridHandler.GroupedItems items){
        var result=RecipeGridHandler.tryToApplyRecipe(level,items);if(result!=null)return result;
        var plan=ArcaneMechanicalCrafting.find((MechanicalCrafterBlockEntity)(Object)this,items);
        if(plan==null)return null;var crafted=plan.result(true);return crafted.isEmpty()?null:crafted;
    }
}
