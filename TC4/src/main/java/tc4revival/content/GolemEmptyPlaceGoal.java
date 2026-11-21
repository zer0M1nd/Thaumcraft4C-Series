package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Simulate a colored destination, commit once, and balance its open/close callbacks. */
public class GolemEmptyPlaceGoal extends Goal {
    private final GolemWorkHost host;
    private final boolean sorting;
    private BlockPos destination,opened;
    private Direction side;
    private int chestTicks;
    public GolemEmptyPlaceGoal(GolemWorkHost host) { this(host,false); }
    public GolemEmptyPlaceGoal(GolemWorkHost host,boolean sorting) {
        this.host=host;this.sorting=sorting;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        destination=null;
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty()) return false;
        for (var pos : GolemMarkedInventories.positions(host,true)) {
            if (GolemInventoryViews.same(level,pos,host.homeInventory())) continue;
            for (byte color : host.filters().matchingColors(host.carried(),host.configuration()))
                for (var face : host.markers().faces(pos,level.dimension().location(),color)) {
                    var inventory=GolemInventoryViews.single(level,pos,face);
                    if (sorting && GolemItemTransfer.extract(inventory,host.carried(),1,host.configuration(),false).isEmpty()) continue;
                    if (GolemItemTransfer.insert(inventory,host.carried(),false).getCount()<host.carried().getCount()) {
                        destination=pos;side=face;return true;
                    }
                }
        }
        return false;
    }
    @Override public void start() {
        chestTicks=0;
        if (!(host.mob().level() instanceof ServerLevel level) || destination==null || !GolemWorkRange.reaches(host,destination,4)) return;
        host.mob().getNavigation().stop();
        var rest=GolemItemTransfer.insert(level,destination,side,host.carried(),true);
        if (rest.getCount()<host.carried().getCount()) {
            host.carried(rest);opened=destination;host.openInventory(opened);chestTicks=5;
        }
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && chestTicks>0; }
    @Override public void tick() { chestTicks--; }
    @Override public void stop() { if(opened!=null){host.closeInventory(opened);opened=null;} }
}
