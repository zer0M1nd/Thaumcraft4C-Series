package tc4revival.content;

import java.util.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.*;

/** Snapshot one cargo type and all reachable collection stops before departure. */
public class GolemGatherGoal extends Goal {
    private final GolemWorkHost host;
    private final GolemTaskRoute route;
    private ItemStack selected=ItemStack.EMPTY;
    private int ticks;
    public GolemGatherGoal(GolemWorkHost host) { this.host=host; route=new GolemTaskRoute(host); setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    private boolean eligible(ItemEntity item) {
        var stack=item.getItem();
        return item.isAlive() && !item.hasPickUpDelay() && !stack.isEmpty()
                && (host.filters().isEmpty() || host.filters().amountNeeded(stack,host.configuration().upgrades(5)>0)>0)
                && (selected.isEmpty() || ItemStack.isSameItemSameTags(selected,stack));
    }
    @Override public boolean canUse() {
        if (!host.workEnabled() || host.carrySpace()<=0 || host.mob().tickCount%Math.max(1,host.workDelay())!=0) return false;
        selected=host.carried();double range=host.configuration().range();
        var home=Vec3.atCenterOf(host.home());
        var candidates=new ArrayList<>(host.mob().level().getEntitiesOfClass(ItemEntity.class,new AABB(host.home()).inflate(range),item->eligible(item) && item.position().distanceToSqr(home)<=range*range));
        candidates.sort(Comparator.comparingDouble(item->item.distanceToSqr(host.mob())));
        if (candidates.isEmpty()) return false;
        if (selected.isEmpty()) selected=candidates.get(0).getItem().copyWithCount(1);
        var stops=new ArrayList<net.minecraft.core.BlockPos>();
        for (var item:candidates) if (eligible(item) && stops.size()<64) stops.add(item.blockPosition());
        return route.plan(stops,0);
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && ticks>0 && !route.empty() && host.carrySpace()>0; }
    @Override public void start() { ticks=1200; route.start(); }
    @Override public void stop() { route.clear(); host.mob().getNavigation().stop(); selected=ItemStack.EMPTY; }
    @Override public void tick() {
        ticks--;
        if (route.target()==null) return;
        var target=Vec3.atBottomCenterOf(route.target());
        host.mob().getLookControl().setLookAt(target.x,target.y,target.z,30,30);
        if (host.mob().position().distanceToSqr(target)>.5 && !host.mob().getNavigation().isDone()) return;
        for (var item:host.mob().level().getEntitiesOfClass(ItemEntity.class,host.mob().getBoundingBox().inflate(1),this::eligible)) {
            if (item.distanceToSqr(host.mob())>1) continue;
            int amount=Math.min(item.getItem().getCount(),Math.min(host.carrySpace(),item.getItem().getMaxStackSize()-host.carried().getCount()));
            if (amount<=0) break;
            var picked=item.getItem().copyWithCount(amount);var remaining=item.getItem().copy();remaining.shrink(amount);
            host.mob().take(item,amount);
            if (remaining.isEmpty()) item.discard(); else item.setItem(remaining);
            var carried=host.carried();if(carried.isEmpty()) carried=picked;else carried.grow(amount);host.carried(carried);
            host.transport().progress();ticks=1200;
            host.mob().level().playSound(null,host.mob().blockPosition(),SoundEvents.ITEM_PICKUP,SoundSource.NEUTRAL,.2f,1);
        }
        route.advance();
    }
}
