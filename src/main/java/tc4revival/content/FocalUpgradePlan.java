package tc4revival.content;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;

/** Ordered preview and server validation share the same dependency checks. */
public record FocalUpgradePlan(ItemStack result, AspectAmounts cost, int experience) {
    /** The visible balance is authoritative; totalExperience can lag level commands/enchanting. */
    public static long availablePoints(Player player) {
        long level = Math.max(0, player.experienceLevel);
        long base = level <= 16 ? level * level + 6 * level
                : level <= 31 ? (5 * level * level - 81 * level + 720) / 2
                : (9 * level * level - 325 * level + 4440) / 2;
        // Vanilla repeatedly divides floats while crossing levels. At an exact
        // boundary it can leave the preceding level with a nearly full bar.
        return base + Math.round((double) player.experienceProgress * player.getXpNeededForNextLevel());
    }
    public static int experienceForCount(int count) {
        if (count < 1 || count > 5) throw new IllegalArgumentException("Upgrade count must be 1..5");
        // Divide the fixed thirty-level point budget, then round UP to a whole
        // displayed level. Charging converts that rounded level back to points.
        int proportionalPoints = (experiencePoints(30) * count + 4) / 5;
        int level = 0;
        while (experiencePoints(level) < proportionalPoints) level++;
        return level;
    }
    /** Fixed XP from level zero, like a machine's experience tank; high-level players pay the same points. */
    public static int experiencePoints(int levels) {
        if (levels < 0) throw new IllegalArgumentException("Negative level");
        if (levels <= 16) return levels * levels + 6 * levels;
        if (levels <= 31) return (5 * levels * levels - 81 * levels + 720) / 2;
        return (9 * levels * levels - 325 * levels + 4440) / 2;
    }
    public static FocalUpgradePlan create(ItemStack original, List<Integer> choices, Player player) {
        if (!(original.getItem() instanceof WandFocusItem item) || original.getCount() != 1
                || choices.isEmpty() || choices.size() > 5) return null;
        var preview = original.copy();
        var cost = new AspectAmounts();
        for (int id : choices) {
            var option = FocusUpgrade.byId(id).orElse(null); int rank = item.nextRank(preview);
            if (option == null || rank > 5 || !item.availableUpgrades(preview, rank).contains(option)) return null;
            if (player != null && (!item.canApplyUpgrade(preview, player, option, rank)
                    || !thaumcraft.api.wands.FocusUpgradeType.canApply(option, preview, player, rank))) return null;
            cost.add(FocalManipulatorBlockEntity.cost(option, rank));
            if (!item.applyUpgrade(preview, option, rank)) return null;
        }
        return new FocalUpgradePlan(preview, cost, experienceForCount(choices.size()));
    }
}
