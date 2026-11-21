package thaumicomniverse.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class CollaborationRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "thaumicomniverse");
    public static final RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<SoulCoreRecipe>> SOUL_CORE = TYPES.register("soul_core", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(SoulCoreRecipe::new));
    public static final RegistryObject<CertificateRecipeSerializer> CERTIFICATE = TYPES.register("certificate", CertificateRecipeSerializer::new);
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
