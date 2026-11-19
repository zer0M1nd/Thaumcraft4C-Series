package conciliummodern.mixin;

import com.google.gson.JsonElement;
import com.ilya3point999k.thaumicconcilium.api.ThaumicConciliumApi;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep normal parsing, conditions, ownership and client recipe synchronization intact. */
@Mixin(value=RecipeManager.class,remap=false)
public abstract class ConciliumRecipeInputMixin {
    @ModifyVariable(method={"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V","m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},at=@At("HEAD"),argsOnly=true,require=1)
    private Map<ResourceLocation,JsonElement> concilium$registeredRecipes(Map<ResourceLocation,JsonElement> recipes){return ThaumicConciliumApi.mergeRecipeInput(recipes);}
}
