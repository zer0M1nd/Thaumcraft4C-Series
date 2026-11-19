package thaumicomniverse.mixin;

import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Select native serializers after registries exist, including Avaritia forks sharing a mod ID. */
@Mixin(value=RecipeManager.class,remap=false)
public abstract class AvaritiaRecipesMixin {
    @ModifyVariable(method={"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},
            at=@At("HEAD"),argsOnly=true,remap=false)
    private Map<ResourceLocation,JsonElement> omniverse$avaritiaRecipes(Map<ResourceLocation,JsonElement> recipes) {
        return thaumicomniverse.compat.AvaritiaRecipes.rewrite(recipes);
    }
}
