package thaumcraft.api.crafting;

import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/** Common-side registry. Register pure deterministic transformations on both physical sides.
 * Recipe JSON: "result_transform": {"type":"addon:preserve_filter", ...custom fields...}.
 * Empty result rejects the craft. Inputs, config and base output are private copies.
 */
public class ArcaneResultTransforms {
    @FunctionalInterface public interface Transform {
        ItemStack assemble(List<ItemStack> inputs, int width, int height, RegistryAccess registries, ItemStack base, JsonObject config);
    }
    private static final Map<ResourceLocation, Transform> REGISTRY = new java.util.concurrent.ConcurrentHashMap<>();
    public static void register(ResourceLocation id, Transform transform) {
        if (REGISTRY.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(transform)) != null)
            throw new IllegalArgumentException("Duplicate arcane result transform " + id);
    }
    public static ItemStack apply(JsonObject config, CraftingContainer grid, RegistryAccess registries, ItemStack base) {
        var id = ResourceLocation.parse(config.get("type").getAsString());
        var transform = REGISTRY.get(id);
        // Unknown addon handlers fail closed, never silently produce an NBT-stripped replacement.
        if (transform == null) return ItemStack.EMPTY;
        var inputs = java.util.stream.IntStream.range(0, grid.getContainerSize()).mapToObj(i -> grid.getItem(i).copy()).toList();
        var result = Objects.requireNonNull(transform.assemble(inputs, grid.getWidth(), grid.getHeight(), registries, base.copy(), config.deepCopy()));
        if (result.isEmpty()) return ItemStack.EMPTY;
        if (result.getCount() > result.getMaxStackSize()) throw new IllegalArgumentException("Oversized arcane result " + id);
        return result.copy();
    }
}
