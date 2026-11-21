package thaumicfutures;
public final class FutureRecipes {
    private static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> TYPES=net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS,"thaumicfutures");
    public static final net.minecraftforge.registries.RegistryObject<thaumicfutures.recipe.RevealingHelmetRecipe.Serializer> REVEALING_HELMET=TYPES.register("revealing_helmet",thaumicfutures.recipe.RevealingHelmetRecipe.Serializer::new);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){TYPES.register(bus);}
}
