package thaumicomniverse.compat.jei;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.content.TCFluids;

@JeiPlugin
public final class OmniverseJei implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("thaumicomniverse:jei"); }
    private boolean create(){return net.minecraftforge.fml.ModList.get().isLoaded("create");}
    @Override public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration r){if(create())thaumicomniverse.compat.create.ArcaneSpoutingJei.categories(r);}
    @Override public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration r){if(create())thaumicomniverse.compat.create.ArcaneSpoutingJei.catalysts(r);}
    @Override public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration r){if(create())thaumicomniverse.compat.create.ArcaneSpoutingJei.registerRecipes(r);}
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime r){
        if(create())thaumicomniverse.compat.create.ArcaneSpoutingJei.runtime(r);

    }
    @Override public void onRuntimeUnavailable(){if(create())thaumicomniverse.compat.create.ArcaneSpoutingJei.close();}
    @Override public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        // Finite death water defaults to one quantum, not a source. JEI's
        // automatic source-fluid scan therefore misses this valid GT input.
        registration.addExtraIngredients(ForgeTypes.FLUID_STACK,List.of(new FluidStack(TCFluids.DEATH.get(),1000)));
    }
}
