package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Prefer a receiving container; non-container markers are bounded toss destinations. */
public class GolemEmptyTravelGoal extends GolemMoveToGoal {
    private final boolean sorting;
    public GolemEmptyTravelGoal(GolemWorkHost host) { this(host, false); }
    public GolemEmptyTravelGoal(GolemWorkHost host, boolean sorting) { super(host); this.sorting = sorting; }
    @Override protected BlockPos destination() {
        if (!(mob.level() instanceof ServerLevel level) || host.carried().isEmpty()) return null;
        var colors = host.filters().matchingColors(host.carried(), host.configuration());
        var positions = new LinkedHashSet<BlockPos>();
        for (byte color : colors) positions.addAll(GolemMarkedInventories.withRoom(host, host.carried(), color));
        double range = host.configuration().range();
        positions.removeIf(pos -> GolemInventoryViews.same(level, pos, host.homeInventory())
                || mob.position().distanceToSqr(Vec3.atCenterOf(pos)) > range * range
                || sorting && colors.stream().flatMap(color -> host.markers().faces(pos, level.dimension().location(), color).stream())
                    .noneMatch(face -> !GolemItemTransfer.extract(GolemInventoryViews.single(level,pos,face),host.carried(),1,host.configuration(),false).isEmpty()));
        if (positions.isEmpty() && !sorting) for (byte color : colors)
            for (var marker : host.markers().inDimension(level.dimension().location())) {
                var pos = marker.pos();
                if ((color == -1 || marker.color() == color) && !pos.equals(host.homeInventory())
                        && !GolemInventoryViews.exists(level, pos) && mob.position().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range)
                    positions.add(pos);
            }
        return positions.stream().min(Comparator.comparingDouble(pos -> mob.position().distanceToSqr(Vec3.atCenterOf(pos)))).orElse(null);
    }
}
