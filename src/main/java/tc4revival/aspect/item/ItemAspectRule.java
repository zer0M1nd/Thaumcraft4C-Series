package tc4revival.aspect.item;

import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

/** Explicit legacy membership prevents modern shared tags from assigning aspects to new content. */
public record ItemAspectRule(List<String> items, String requiredTag, List<Quantity> aspects,boolean generated,ItemNbtPredicate nbt) {
    public ItemAspectRule(List<String> items,String requiredTag,List<Quantity> aspects,boolean generated){this(items,requiredTag,aspects,generated,ItemNbtPredicate.EMPTY);}
    public ItemAspectRule(List<String> items,String requiredTag,List<Quantity> aspects){this(items,requiredTag,aspects,false);}
    public ItemAspectRule {
        items = List.copyOf(items);
        aspects = List.copyOf(aspects);
        if(nbt==null)nbt=ItemNbtPredicate.EMPTY;
        if (items.isEmpty() || new HashSet<>(items).size() != items.size()) {
            throw new IllegalArgumentException("Empty or duplicate item membership");
        }
        items.forEach(id->{if(!id.equals("*"))checkId(id);});
        if(items.contains("*") && (items.size()!=1 || requiredTag==null))throw new IllegalArgumentException("Wildcard membership requires exactly one tag");
        if (requiredTag != null) checkId(requiredTag);
        if (new HashSet<>(aspects.stream().map(Quantity::aspect).toList()).size() != aspects.size()) {
            throw new IllegalArgumentException("Duplicate aspect quantity");
        }
    }

    public boolean matches(String item, Predicate<String> hasTag) {
        return matches(item,hasTag,null);
    }
    public boolean matches(String item, Predicate<String> hasTag, net.minecraft.nbt.CompoundTag tag) {
        return (items.contains(item)||items.contains("*")) && (requiredTag == null || hasTag.test(requiredTag)) && nbt.matches(tag);
    }

    public AspectAmounts amounts(AspectCatalog catalog) {
        return amounts(catalog,tc4revival.aspect.recipe.InferenceLimits.server());
    }
    public AspectAmounts amounts(AspectCatalog catalog,tc4revival.aspect.recipe.InferenceLimits limits) {
        var result = new AspectAmounts();
        aspects.forEach(quantity -> catalog.find(quantity.aspect()).ifPresent(aspect -> result.add(aspect, quantity.amount())));
        return generated?tc4revival.aspect.recipe.GeneratedAspectLimit.apply(result,catalog,limits):result;
    }

    private static void checkId(String id) {
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid resource id: " + id);
    }

    public record Quantity(String aspect, int amount) {
        public Quantity {
            if (aspect == null || !aspect.matches("[a-z0-9_:./-]+") || amount < 0 || amount > 10000) {
                throw new IllegalArgumentException("Invalid aspect quantity");
            }
        }
    }
}
