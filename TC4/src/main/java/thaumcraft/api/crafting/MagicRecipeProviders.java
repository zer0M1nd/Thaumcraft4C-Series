package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.BiFunction;
import net.minecraft.world.level.Level;
import tc4revival.content.*;
import thaumcraft.api.util.ExtensionRegistry;

/** Pure priority candidates preceding the ordinary recipe manager. No recipe cache survives a reload. */
public final class MagicRecipeProviders {
    private static final ExtensionRegistry<BiFunction<Level,CrucibleRecipe.Input,Optional<CrucibleRecipe>>> CRUCIBLE=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiFunction<Level,InfusionRecipe.Input,Optional<InfusionRecipe>>> INFUSION=new ExtensionRegistry<>();
    private MagicRecipeProviders() {}
    public static void registerCrucible(String id,int priority,BiFunction<Level,CrucibleRecipe.Input,Optional<CrucibleRecipe>> provider){CRUCIBLE.register(id,priority,provider);}
    public static void registerInfusion(String id,int priority,BiFunction<Level,InfusionRecipe.Input,Optional<InfusionRecipe>> provider){INFUSION.register(id,priority,provider);}
    public static Optional<CrucibleRecipe> crucible(Level level,CrucibleRecipe.Input input){
        for(var entry:CRUCIBLE.entries()){
            var copy=new CrucibleRecipe.Input(input.getItem(0).copy(),input.aspects(),input::researchKnown);
            var candidate=Objects.requireNonNull(entry.value().apply(level,copy));
            if(candidate.isPresent()&&candidate.get().matches(input,level))return candidate;
        }
        return Optional.empty();
    }
    public static Optional<InfusionRecipe> infusion(Level level,InfusionRecipe.Input input){
        for(var entry:INFUSION.entries()){
            var candidate=Objects.requireNonNull(entry.value().apply(level,input.snapshot()));
            if(candidate.isPresent()&&candidate.get().matches(input,level))return candidate;
        }
        return Optional.empty();
    }
}
