package tc4revival.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import tc4revival.client.search.AspectExpression.Values;

/** JEI supplies visible ingredients in display order. Evaluate each distinct composition once per expression. */
public final class JeiAspectIndex {
    private final List<ITypedIngredient<?>> items = new ArrayList<>();
    private final IntArrayList groups = new IntArrayList();
    private final List<Values> profiles = new ArrayList<>();
    private Iterator<ITypedIngredient<?>> pending;
    private HashMap<Values, Integer> ids = new HashMap<>();
    private final Function<ItemStack, Values> resolve;

    public JeiAspectIndex(Stream<ITypedIngredient<?>> visible, Function<ItemStack, Values> resolve) {
        this.pending = visible.sequential().iterator();
        this.resolve = resolve;
    }

    public boolean complete() { return pending == null; }

    /** Resolve on the client thread: addon item/capability readers need not be thread safe. */
    public void advance(long budgetNanos) {
        if (complete()) return;
        long start = System.nanoTime();
        do {
            if (!pending.hasNext()) {
                pending = null;
                ids = null;
                return;
            }
            var ingredient = pending.next();
            var stack = ingredient.getItemStack();
            if (stack.isPresent()) {
                var values = resolve.apply(stack.get());
                if (values != null) {
                    int group = ids.computeIfAbsent(values, profile -> {
                        profiles.add(profile);
                        return profiles.size() - 1;
                    });
                    items.add(ingredient);
                    groups.add(group);
                }
            }
        } while (System.nanoTime() - start < budgetNanos);
    }

    public Stream<ITypedIngredient<?>> filter(Predicate<Values> query) {
        var selected = new boolean[profiles.size()];
        for (int i = 0; i < selected.length; i++) selected[i] = query.test(profiles.get(i));
        return java.util.stream.IntStream.range(0, items.size())
                .filter(i -> selected[groups.getInt(i)]).mapToObj(items::get);
    }
}
