package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Home insertion: first simulate, then commit and keep the chest open for five ticks. */
public  class GolemHomePlaceGoal extends Goal {
    private final GolemWorkHost host;
    private int chestTicks;
    private BlockPos opened;
    public GolemHomePlaceGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty() || !host.nearHomeAndIdle()
                || host.mob().tickCount%Math.max(1,host.workDelay())!=0) return false;
        var pos=host.homeInventory();
        return !ItemStack.matches(GolemItemTransfer.insert(level,pos,host.homeFacing(),host.carried(),false),host.carried());
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && chestTicks>0; }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty()) return;
        var pos=host.homeInventory();
        var remainder=GolemItemTransfer.insert(level,pos,host.homeFacing(),host.carried(),true);
        if (!ItemStack.matches(remainder,host.carried())) {
            host.carried(remainder); host.openInventory(pos); opened=pos.immutable(); chestTicks=5;
        }
    }
    @Override public void tick() { chestTicks--; }
    @Override public void stop() { if (opened!=null) { host.closeInventory(opened); opened=null; } }
}
