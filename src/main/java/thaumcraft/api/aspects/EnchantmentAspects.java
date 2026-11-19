package thaumcraft.api.aspects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
/** Register during common setup. Every registered enchantment contributes its levels to magic;
 * this registry supplies an optional additional aspect, using legacy max/merge semantics. */
public class EnchantmentAspects {
    private static final Map<String,String> SPECIAL = new ConcurrentHashMap<>();
    private EnchantmentAspects() {}
    public static void register(ResourceLocation enchantment, String aspect) {
        java.util.Objects.requireNonNull(aspect);
        if(SPECIAL.putIfAbsent(enchantment.toString(),aspect)!=null)
            throw new IllegalArgumentException("Duplicate enchantment aspect: "+enchantment);
    }
    public static String special(String enchantment) {
        return SPECIAL.getOrDefault(enchantment,tc4revival.aspect.item.LegacyEnchantmentBonuses.aspect(enchantment));
    }
}
