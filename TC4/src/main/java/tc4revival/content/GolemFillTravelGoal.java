package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class GolemFillTravelGoal extends GolemMoveToGoal {
    public GolemFillTravelGoal(GolemWorkHost host) { super(host); }
    @Override protected BlockPos destination() {
        if (!host.carried().isEmpty() || host.filters().isEmpty()) return null;
        var missing = host.missingItems();
        if (missing == null) return null;
        BlockPos nearest = null;
        double best = Double.MAX_VALUE, range = host.configuration().range();
        ItemStack chosen = ItemStack.EMPTY;
        for (var stack : missing) {
            var positions = new LinkedHashSet<BlockPos>();
            for (byte color : host.filters().matchingColors(stack, host.configuration()))
                positions.addAll(GolemMarkedInventories.withGoods(host, stack, color));
            for (var pos : positions) {
                double distance = mob.position().distanceToSqr(Vec3.atCenterOf(pos));
                if (mob.level() instanceof net.minecraft.server.level.ServerLevel level && GolemInventoryViews.same(level,pos,host.homeInventory())
                        || distance > range * range || distance >= best) continue;
                nearest = pos; best = distance; chosen = stack;
            }
        }
        host.watched(chosen.copy());
        return nearest;
    }
}
