package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Toss delivery uses marker order, not nearest-distance sorting or marker face. */
public  class GolemEmptyDropGoal extends Goal {
    private final GolemWorkHost host;
    private int ticks;
    public GolemEmptyDropGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    private BlockPos target() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || host.carried().isEmpty()
                || !host.mob().getNavigation().isDone()) return null;
        for (byte color:host.filters().matchingColors(host.carried(),host.configuration())) {
            for (var marker:host.markers().inDimension(level.dimension().location())) {
                if (color!=-1 && marker.color()!=color) continue;
                if (GolemInventoryViews.exists(level,marker.pos())
                        || !GolemWorkRange.reaches(host,marker.pos(),4)) continue;
                if (marker.pos().equals(host.homeInventory())) continue;
                return marker.pos();
            }
        }
        return null;
    }
    @Override public boolean canUse() { return target()!=null; }
    @Override public void start() {
        ticks=200;
        var pos=target();
        if (pos==null || !(host.mob().level() instanceof ServerLevel level)) return;
        var mob=host.mob();
        var item=new ItemEntity(level,mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ(),host.carried().copy());
        double distance=mob.position().distanceTo(Vec3.atCenterOf(pos));
        item.setDeltaMovement((pos.getX()+.5-mob.getX())*distance/3,
                .1+(pos.getY()+.5-item.getY())*distance/3,(pos.getZ()+.5-mob.getZ())*distance/3);
        item.setPickUpDelay(10);
        if (level.addFreshEntity(item)) { host.carried(ItemStack.EMPTY); host.action(); }
    }
    @Override public boolean canContinueToUse() { return ticks>0 && canUse(); }
    @Override public void tick() { ticks--; }
}
