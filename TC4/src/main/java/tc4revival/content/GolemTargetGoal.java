package tc4revival.content;

import java.util.Comparator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** Native combat eligibility; butchery additionally leaves two eligible animals of each class. */
public class GolemTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final GolemWorkHost host;
    private final boolean butcher;
    public GolemTargetGoal(GolemWorkHost host, boolean butcher) {
        super(host.mob(), LivingEntity.class, 0, true, false, candidate -> GolemCombatTargets.valid(host, candidate));
        this.host = host;
        this.butcher = butcher;
        targetConditions.range(host.configuration().range());
    }
    @Override protected double getFollowDistance() {
        return host == null ? super.getFollowDistance() : host.configuration().range();
    }
    @Override public boolean canUse() { return host.workEnabled() && super.canUse(); }
    @Override protected void findTarget() {
        if (!butcher) { super.findTarget(); return; }
        target = null;
        var animals = host.mob().level().getEntitiesOfClass(LivingEntity.class, getTargetSearchArea(getFollowDistance()),
                candidate -> targetConditions.test(host.mob(), candidate));
        animals.sort(Comparator.comparingInt((LivingEntity entity) -> entity.tickCount).reversed());
        for (var candidate : animals) {
            if (animals.stream().filter(candidate.getClass()::isInstance).count() > 2) { target = candidate; return; }
        }
    }
    @Override public boolean canContinueToUse() {
        var current = host.mob().getTarget();
        return host.workEnabled() && current != null && GolemCombatTargets.valid(host, current) && super.canContinueToUse();
    }
}
