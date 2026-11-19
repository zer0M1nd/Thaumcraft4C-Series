package thaumcraft.api.crafting;

import java.util.function.ToIntFunction;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import tc4revival.content.LegacyRandomEnchantments;

/** Optional integrations supply their current effective enchanting cap, before the final infusion level. */
public class InfusionEnchantmentLimits {
    private static ToIntFunction<Enchantment> external;
    public static void register(ToIntFunction<Enchantment> levels) {
        if (external != null) throw new IllegalStateException("Infusion enchantment cap provider already registered");
        external = java.util.Objects.requireNonNull(levels);
    }
    public static int maximum(Enchantment enchantment, boolean enhanced) {
        if (external != null) return Math.max(1, Math.min(256, external.applyAsInt(enchantment) + 1));
        if (!enhanced) return LegacyRandomEnchantments.definition(enchantment).map(value -> value.maximumLevel).orElseGet(enchantment::getMaxLevel);
        int max = enchantment.getMaxLevel();
        return max == 1 && enchantment != Enchantments.MENDING ? 1 : Math.min(256, max + 1);
    }
    public static boolean requiresVoidSeed(Enchantment enchantment, int target, boolean enhanced) {
        return target == maximum(enchantment, enhanced) && (external != null || enhanced && target > enchantment.getMaxLevel());
    }
}
