package thaumcraft.api.wands;

import java.util.Objects;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Additional per-stack recharge restrictions. Register pure predicates in common setup on both sides.
 * A matching veto blocks recharge; predicates see a private copy and must have no side effects.
 */
public class WandRechargePolicies {
    private static final Map<ResourceLocation, Predicate<ItemStack>> VETOES = new java.util.concurrent.ConcurrentHashMap<>();
    public static void registerVeto(ResourceLocation id, Predicate<ItemStack> veto) {
        if (VETOES.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(veto)) != null)
            throw new IllegalArgumentException("Duplicate recharge policy " + id);
    }
    public static boolean allows(ItemStack stack) {
        for (var veto : VETOES.values()) if (veto.test(stack.copy())) return false;
        return true;
    }
}
