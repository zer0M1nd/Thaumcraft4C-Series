package thaumicomniverse.mixin;

import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Create also detects IC2 Experimental, which does not register its Classic metal IDs. */
@Mixin(value=RecipeManager.class,remap=false)
public abstract class CreateIc2RecipesMixin {
    @ModifyVariable(method={"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},
            at=@At("HEAD"),argsOnly=true,remap=false)
    private Map<ResourceLocation,JsonElement> omniverse$availableIc2Metals(Map<ResourceLocation,JsonElement> recipes) {
        Map<ResourceLocation,JsonElement> result=null;
        for(var entry:recipes.entrySet()) {
            var id=entry.getKey();
            if(!id.getNamespace().equals("create")||!id.getPath().endsWith("_compat_ic2")||!entry.getValue().isJsonObject())continue;
            var output=entry.getValue().getAsJsonObject().get("result");
            if(output==null||!output.isJsonPrimitive()||!output.getAsJsonPrimitive().isString())continue;
            var item=ResourceLocation.tryParse(output.getAsString());
            if(item==null||!item.getNamespace().equals("ic2")||ForgeRegistries.ITEMS.containsKey(item))continue;
            if(result==null)result=new LinkedHashMap<>(recipes);
            result.remove(id);
        }
        return result==null?recipes:result;
    }
}
