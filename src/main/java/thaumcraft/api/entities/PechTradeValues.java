package thaumcraft.api.entities;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** Register during common setup on both sides. Higher priority wins; ties use lexical IDs.
 * Ingredients evaluate current item tags, so tag reloads need no re-registration.
 */
public class PechTradeValues {
    private record Entry(ResourceLocation id, int priority, Ingredient ingredient, int value) {}
    private static volatile List<Entry> entries = List.of();
    public static synchronized void register(ResourceLocation id, int priority, Ingredient ingredient, int value) {
        Objects.requireNonNull(id); Objects.requireNonNull(ingredient);
        if (value < 1 || value > 32) throw new IllegalArgumentException("Pech value must be 1..32");
        var next = new ArrayList<>(entries);
        if (next.stream().anyMatch(e -> e.id.equals(id))) throw new IllegalArgumentException("Duplicate Pech value " + id);
        next.add(new Entry(id, priority, ingredient, value));
        next.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e -> e.id.toString()));
        entries = List.copyOf(next);
    }
    public static void register(ResourceLocation id, int priority, Item item, int value) { register(id, priority, Ingredient.of(item), value); }
    public static void register(ResourceLocation id, int priority, TagKey<Item> tag, int value) { register(id, priority, Ingredient.of(tag), value); }
    public static OptionalInt find(ItemStack stack) {
        if (!stack.isEmpty()) for (var entry : entries) if (entry.ingredient.test(stack)) return OptionalInt.of(entry.value);
        return OptionalInt.empty();
    }
}
