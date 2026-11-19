package thaumcraft.api.research;

import java.util.*;
import java.util.function.*;
import tc4revival.research.*;
import thaumcraft.api.util.ExtensionRegistry;

public final class ResearchRecipeViews {
    private static final ExtensionRegistry<UnaryOperator<ResearchPageRecipes.Entry>> RECIPES=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiPredicate<String,ResearchPage>> PAGES=new ExtensionRegistry<>();
    private ResearchRecipeViews() {}
    public static void registerRecipeView(String id,int priority,UnaryOperator<ResearchPageRecipes.Entry> provider){RECIPES.register(id,priority,provider);}
    public static void registerPageVisibility(String id,int priority,BiPredicate<String,ResearchPage> visible){PAGES.register(id,priority,visible);}
    public static ResearchPageRecipes.Entry recipe(ResearchPageRecipes.Entry original){
        if(net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer())return original;
        var result=original;for(var entry:RECIPES.entries())result=Objects.requireNonNull(entry.value().apply(result));return result;
    }
    public static List<ResearchPage> visible(String research,List<ResearchPage> pages){
        if(net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer())return List.copyOf(pages);
        return pages.stream().filter(page->PAGES.entries().stream().allMatch(entry->entry.value().test(research,page))).toList();
    }
}
