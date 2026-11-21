package thaumcraft.api.research;

import java.util.*;
import java.util.function.*;
import tc4revival.research.ResearchCatalog;
import thaumcraft.api.util.ExtensionRegistry;

/** Display preferences never rewrite the authoritative research definition or its fingerprint. */
public final class ResearchPresentation {
    private record Order(String category,int rank) {}
    private record Background(String category,Supplier<Optional<String>> texture) {}
    private static final ExtensionRegistry<Order> ORDER=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Background> BACKGROUNDS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BooleanSupplier> SEARCH=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiPredicate<String,tc4revival.research.ResearchBrowser.Node>> SEARCH_FILTERS=new ExtensionRegistry<>();
    public static void registerSearch(String id,int priority,BooleanSupplier enabled,BiPredicate<String,tc4revival.research.ResearchBrowser.Node> include){
        SEARCH.register(id,priority,enabled);SEARCH_FILTERS.register(id,priority,include);
    }
    public static boolean searchEnabled(boolean fallback){return fallback||SEARCH.entries().stream().anyMatch(entry->entry.value().getAsBoolean());}
    public static List<tc4revival.research.ResearchBrowser.Node> search(String category,List<tc4revival.research.ResearchBrowser.Node> candidates){
        return candidates.stream().filter(node->SEARCH_FILTERS.entries().stream().allMatch(entry->entry.value().test(category,node))).toList();
    }
    private ResearchPresentation() {}
    public static void registerCategoryOrder(String id,int priority,String category,int rank){ORDER.register(id,priority,new Order(category,rank));}
    public static void registerBackground(String id,int priority,String category,Supplier<Optional<String>> texture){BACKGROUNDS.register(id,priority,new Background(category,texture));}
    public static List<ResearchCatalog.Category> order(List<ResearchCatalog.Category> categories){
        return categories.stream().sorted(Comparator.<ResearchCatalog.Category>comparingInt(category ->
                category.key().equals("TUTORIAL") ? 0 : 1).thenComparingInt(category->{
            for(var entry:ORDER.entries())if(entry.value().category().equals(category.key()))return entry.value().rank();
            return 0;
        })).toList();
    }
    public static String background(String category,String fallback){
        for(var entry:BACKGROUNDS.entries())if(entry.value().category().equals(category)){
            var result=entry.value().texture().get();if(result.isPresent())return result.get();
        }
        return fallback;
    }
}
