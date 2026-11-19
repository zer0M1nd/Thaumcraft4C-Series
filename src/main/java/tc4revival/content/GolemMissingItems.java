package tc4revival.content;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Combine matching demands, then account for each native inventory slot once. */
public class GolemMissingItems {
    private GolemMissingItems() {}
    public static List<ItemStack> calculate(GolemWorkHost host) {
        var result = new ArrayList<ItemStack>(); var config = host.configuration();
        for (int slot = 0; slot < host.filters().size(); slot++) {
            var sample = host.filters().sample(slot); if (sample.isEmpty()) continue;
            var match = result.stream().filter(stack -> GolemItemMatching.matches(stack,sample,config)).findFirst();
            if (match.isPresent()) match.get().setCount((int)Math.min(Integer.MAX_VALUE,(long)match.get().getCount()+sample.getCount()));
            else result.add(sample.copy());
        }
        if (config.toggle(0)) return result;
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var inventory = GolemInventoryViews.single(level,host.homeInventory(),host.homeFacing());
        if (inventory == null) return null;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var actual = inventory.getStackInSlot(slot); int available = actual.getCount();
            for (var sample : result) {
                if (available <= 0 || sample.isEmpty() || !GolemItemMatching.matches(actual,sample,config)) continue;
                int counted = Math.min(available,sample.getCount()); sample.shrink(counted); available -= counted;
            }
        }
        result.removeIf(ItemStack::isEmpty); return result;
    }
}
