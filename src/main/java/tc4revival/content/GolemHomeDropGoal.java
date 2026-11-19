package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Toss at the home block only when it has no inventory, never when its inventory is merely full. */
public  class GolemHomeDropGoal extends Goal {
    private final GolemWorkHost host;
    private int ticks;
    public GolemHomeDropGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty() || !host.nearHomeAndIdle()) return false;
        var pos=host.homeInventory();
        return level.hasChunkAt(pos) && !GolemInventoryViews.exists(level,pos);
    }
    @Override public boolean canContinueToUse() { return ticks>0 && canUse(); }
    @Override public void tick() { ticks--; }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty()) return;
        ticks=200; var mob=host.mob(); var home=host.homeInventory();
        var drop=new ItemEntity(level,mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ(),host.carried().copy());
        double distance=Math.sqrt(mob.distanceToSqr(home.getX()+.5,home.getY()+.5,home.getZ()+.5));
        drop.setDeltaMovement((home.getX()+.5-mob.getX())*(distance/3),
                .1+(home.getY()+.5-(mob.getY()+mob.getBbHeight()/2))*(distance/3),
                (home.getZ()+.5-mob.getZ())*(distance/3));
        drop.setPickUpDelay(10);
        if (level.addFreshEntity(drop)) { host.carried(ItemStack.EMPTY); host.action(); }
    }
}
