package thmodern.focus;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;
public final class FocusRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,thmodern.Horizons.ID);
    public static final RegistryObject<RecipeSerializer<thmodern.alchemy.VoidPuttyRepairRecipe>> VOID_REPAIR=SERIALIZERS.register("void_putty_repair",()->new SimpleCraftingRecipeSerializer<>(thmodern.alchemy.VoidPuttyRepairRecipe::new));
    public static final RegistryObject<RecipeSerializer<IlluminationDyeRecipe>> DYE=SERIALIZERS.register("illumination_dye",()->new SimpleCraftingRecipeSerializer<>(IlluminationDyeRecipe::new));
}

