package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Return goods whose destination disappeared or is temporarily excluded. */
public  class GolemHomeReplaceGoal extends Goal {
    private final GolemWorkHost host;
    private int chestTicks;
    private BlockPos opened;
    public GolemHomeReplaceGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (host.carried().isEmpty() || !host.nearHomeAndIdle() || host.mob().tickCount%Math.max(1,host.workDelay())!=0) return false;
        if (host.transport().timedOut(host.carried())) return true;
        var core=host.configuration().core();
        if (core==GolemCoreType.EMPTY || core==GolemCoreType.USE || core==GolemCoreType.SORTING)
            return !host.transport().findTarget(host.carried());
        var needed=host.transport().needed();
        if (needed==null || needed.isEmpty()) return false;
        return needed.stream().noneMatch(stack->GolemItemMatching.matches(stack,host.carried(),host.configuration()));
    }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level)) return;
        var pos=host.homeInventory();
        for (int half=0;half<2 && pos!=null;half++,pos=GolemInventoryViews.partner(level,pos)) {
            var rest=GolemItemTransfer.insert(level,pos,host.homeFacing(),host.carried(),true);
            if (ItemStack.matches(rest,host.carried())) continue;
            host.carried(rest); host.openInventory(pos); opened=pos; chestTicks=5; break;
        }
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && (canUse() || chestTicks>0); }
    @Override public void tick() { chestTicks--; }
    @Override public void stop() { if (opened!=null) { host.closeInventory(opened); opened=null; } }
}
