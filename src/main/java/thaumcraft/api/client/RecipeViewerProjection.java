package thaumcraft.api.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.BrowserRecipe;
import thaumcraft.api.util.ExtensionRegistry;

/** Recipe-viewer display filters; no optional viewer types and no mutation of authoritative recipes. */
public final class RecipeViewerProjection {
    private record Policy(Supplier<String> revision,Predicate<String> aspect,Predicate<ItemStack> item,Function<BrowserRecipe,Optional<BrowserRecipe>> recipe) {}
    private static final ExtensionRegistry<Policy> POLICIES=new ExtensionRegistry<>();
    private RecipeViewerProjection() {}
    public static void register(String id,int priority,Supplier<String> revision,Predicate<String> aspect,Predicate<ItemStack> item,Function<BrowserRecipe,Optional<BrowserRecipe>> recipe){POLICIES.register(id,priority,new Policy(revision,aspect,item,recipe));}
    public static String revision(){return POLICIES.entries().stream().map(e->e.id()+"="+e.value().revision().get()).collect(java.util.stream.Collectors.joining(";"));}
    public static boolean aspect(String tag){return POLICIES.entries().stream().allMatch(e->e.value().aspect().test(tag));}
    public static boolean item(ItemStack stack){return POLICIES.entries().stream().allMatch(e->e.value().item().test(stack.copy()));}
    public static Optional<BrowserRecipe> recipe(BrowserRecipe raw){
        var current=Optional.of(raw);for(var entry:POLICIES.entries()){if(current.isEmpty())break;current=Objects.requireNonNull(entry.value().recipe().apply(current.get()));}return current;
    }
}
