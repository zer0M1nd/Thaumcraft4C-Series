package thaumicomniverse.compat.create;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ArcaneAssembly {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "thaumicomniverse");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "thaumicomniverse");
    public static final Type CRUCIBLE = new Type("crucible_processing", false);
    public static final Type INFUSION = new Type("infusion_processing", true);
    public static final Type ESSENTIA = new Type("essentia_spouting",false,true);
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }

    public static class Type implements IRecipeTypeInfo {
        private final ResourceLocation id;
        public final RegistryObject<RecipeType<ArcaneProcessingRecipe>> type;
        public final RegistryObject<ProcessingRecipeSerializer<ArcaneProcessingRecipe>> serializer;
        private Type(String name, boolean infusion) {
            this(name,infusion,false);
        }
        private Type(String name,boolean infusion,boolean spouting){
            id = ResourceLocation.parse("thaumicomniverse:" + name);
            type = TYPES.register(name, () -> new RecipeType<>() { @Override public String toString() { return id.toString(); } });
            serializer = SERIALIZERS.register(name, () -> new ProcessingRecipeSerializer<>(params -> new ArcaneProcessingRecipe(this, params, infusion,spouting)));
        }
        @Override public ResourceLocation getId() { return id; }
        @Override @SuppressWarnings("unchecked") public <T extends RecipeSerializer<?>> T getSerializer() { return (T) serializer.get(); }
        @Override @SuppressWarnings("unchecked") public <T extends RecipeType<?>> T getType() { return (T) type.get(); }
    }
}
