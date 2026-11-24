package tc4revival.mixin;

import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Forge world configs arrive after the first recipe reload; defer only registered config conditions. */
@Mixin(value=RecipeManager.class, remap=false)
public abstract class FeatureRecipeConditionsMixin {
    @Shadow(remap=false) @Final private ICondition.IContext context;

    @ModifyVariable(method={"apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},
            at=@At("HEAD"), argsOnly=true, remap=false)
    private Map<ResourceLocation, JsonElement> thaumcraft$deferConfigConditions(Map<ResourceLocation, JsonElement> recipes) {
        return thaumcraft.api.config.FeatureRecipes.prepare((RecipeManager)(Object)this, recipes, context);
    }
    @Redirect(method="fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;Lnet/minecraftforge/common/crafting/conditions/ICondition$IContext;)Lnet/minecraft/world/item/crafting/Recipe;",
            at=@At(value="INVOKE", target="Lnet/minecraft/world/item/crafting/RecipeSerializer;fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;Lnet/minecraftforge/common/crafting/conditions/ICondition$IContext;)Lnet/minecraft/world/item/crafting/Recipe;"), remap=false)
    private static net.minecraft.world.item.crafting.Recipe<?> thaumcraft$readMaterialInputs(
            net.minecraft.world.item.crafting.RecipeSerializer<?> serializer, ResourceLocation id,
            com.google.gson.JsonObject json, ICondition.IContext context) {
        return tc4revival.content.MetalRecipeIngredients.read(context, () -> serializer.fromJson(id, json, context));
    }

}
