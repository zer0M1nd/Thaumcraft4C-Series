package thaumicomniverse.compat;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import dev.shadowsoffire.apotheosis.ench.EnchModule;
import tc4revival.content.LegacyRepairEnchantment;

/** Eligibility belongs to each enchantment; table-only discovery restrictions are lifted for the TC family. */
public class ApotheosisEnchantments {
    public static final Set<String> PROJECTS = Set.of("thaumcraft", "automagy", "emt", "forbidden_magic", "gadomancy", "nodalmechanics", "taintedmagic", "thaumicbases", "thaumicenergistics", "thaumichorizons", "thaumicinfusion", "thaumicomniverse", "thaumic_tinkerer", "warptheory", "witchinggadgets", "thaumicjourney", "thaumicexpedition");
    private static final ThreadLocal<Boolean> TREASURE_SHELF = ThreadLocal.withInitial(() -> false);
    public static boolean repairOverride(ItemStack stack) { return TREASURE_SHELF.get() && stack.isDamageableItem(); }
    public static boolean projectEligible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (var enchantment : ForgeRegistries.ENCHANTMENTS) {
            var id=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if(id!=null && PROJECTS.contains(id.getNamespace()) && enchantment.canEnchant(stack)) return true;
        }
        return false;
    }
    public static List<EnchantmentInstance> select(boolean shelf, net.minecraft.util.RandomSource random, ItemStack stack, int power, float quanta, float arcana, float rectification, boolean treasure, Set<Enchantment> blacklist) {
        boolean previous=TREASURE_SHELF.get(); TREASURE_SHELF.set(shelf);
        try { return dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper.selectEnchantment(random,stack,power,quanta,arcana,rectification,treasure,blacklist); }
        finally { if(previous) TREASURE_SHELF.set(true); else TREASURE_SHELF.remove(); }
    }
    public static boolean hasShelf(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var shelf=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("apotheosis:treasure_shelf"));
        if (shelf==null) return false;
        for(var offset:net.minecraft.world.level.block.EnchantmentTableBlock.BOOKSHELF_OFFSETS)
            if(dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.canReadStatsFrom(level,pos,offset) && level.getBlockState(pos.offset(offset)).is(shelf)) return true;
        return false;
    }
    public static void setup() {
        thaumcraft.api.crafting.InfusionEnchantmentLimits.register(enchantment -> EnchModule.getEnchInfo(enchantment).getMaxLevel());
    }
    public static List<EnchantmentInstance> additional(List<EnchantmentInstance> original, int power, ItemStack stack, boolean treasure, Set<Enchantment> blacklist) {
        var result = new ArrayList<>(original);
        for (var enchantment : ForgeRegistries.ENCHANTMENTS) {
            var id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (id == null || !PROJECTS.contains(id.getNamespace()) || blacklist.contains(enchantment)
                    || result.stream().anyMatch(entry -> entry.enchantment == enchantment)) continue;
            boolean eligible = enchantment.canEnchant(stack);
            if (enchantment instanceof LegacyRepairEnchantment && repairOverride(stack)) eligible = true;
            if (!eligible) continue;
            var info = EnchModule.getEnchInfo(enchantment);
            for (int level = info.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
                if (power < info.getMinPower(level) || power > info.getMaxPower(level)) continue;
                result.add(new EnchantmentInstance(enchantment, level)); break;
            }
        }
        return result;
    }
}
