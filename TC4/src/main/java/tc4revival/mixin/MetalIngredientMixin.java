package tc4revival.mixin;

import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tc4revival.content.MetalRecipeIngredients;

@Mixin(value = VanillaIngredientSerializer.class, remap = false)
public abstract class MetalIngredientMixin {
    @ModifyVariable(method = "parse(Lcom/google/gson/JsonObject;)Lnet/minecraft/world/item/crafting/Ingredient;",
            at = @At("HEAD"), argsOnly = true, remap = false)
    private JsonObject thaumcraft$metalInput(JsonObject input) {
        return MetalRecipeIngredients.readIngredient(input);
    }
}
