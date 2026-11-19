package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

/** A bounded, departure-time snapshot. New work never interrupts an active itinerary. */
public final class GolemTaskRoute {
    public record Leg(BlockPos destination,Path path) {}
    private final GolemWorkHost host;
    private final ArrayDeque<Leg> legs=new ArrayDeque<>();
    public GolemTaskRoute(GolemWorkHost host) { this.host=host; }
    public boolean plan(Collection<BlockPos> candidates) {
        return plan(candidates,1);
    }
    public boolean plan(Collection<BlockPos> candidates,int accuracy) {
        legs.clear();
        if (!(host.mob().getNavigation() instanceof GolemDoorNavigation navigation)) return false;
        var remaining=new ArrayList<>(new LinkedHashSet<>(candidates));
        var start=host.mob().blockPosition();int nodes=0,attempts=0;
        // Inaccessible candidates count too: a sealed plantation must not run thousands of
        // failed A* searches in one server tick merely because no leg was accepted.
        while (!remaining.isEmpty() && legs.size()<64 && nodes<1024 && attempts++<128) {
            final var origin=start;
            var next=remaining.stream().min(Comparator.comparingDouble(origin::distSqr)).orElseThrow();
            remaining.remove(next);
            if (!host.mob().level().hasChunkAt(next)) continue;
            var path=navigation.planFrom(start,next,accuracy);
            if (path==null || !path.canReach() || path.getNodeCount()==0) continue;
            legs.add(new Leg(next.immutable(),path));nodes+=path.getNodeCount();
            start=path.getEndNode().asBlockPos();
        }
        return !legs.isEmpty();
    }
    public boolean empty() { return legs.isEmpty(); }
    public BlockPos target() { return legs.isEmpty()?null:legs.peek().destination(); }
    public void start() {
        if (!legs.isEmpty()) host.mob().getNavigation().moveTo(legs.peek().path(),host.navigationSpeed());
        publish();
    }
    public void advance() { if (!legs.isEmpty()) legs.remove(); start(); }
    public void clear() { legs.clear(); publish(); }
    private void publish() {
        if (host instanceof GolemEntity golem) golem.taskRoute(legs.stream().map(Leg::path).toList());
    }
}
