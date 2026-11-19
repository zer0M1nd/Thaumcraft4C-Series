package tc4revival.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Native pursuit, reach and cooldown, with the golem's work restrictions and arm animation. */
public class GolemMeleeGoal extends MeleeAttackGoal {
    private final GolemWorkHost host;
    public GolemMeleeGoal(GolemWorkHost host) {
        super(host.mob(), host.navigationSpeed(), true);
        this.host = host;
    }
    private boolean eligible() {
        var target = host.mob().getTarget();
        return host.workEnabled() && target != null && GolemCombatTargets.valid(host, target);
    }
    @Override public boolean canUse() { return eligible() && super.canUse(); }
    @Override public boolean canContinueToUse() { return eligible() && super.canContinueToUse(); }
    @Override public void tick() {
        super.tick();
        host.mob().getNavigation().setSpeedModifier(host.navigationSpeed());
    }
    @Override protected void checkAndPerformAttack(LivingEntity target, double distance) {
        if (!host.mob().getSensing().hasLineOfSight(target)) return;
        if (distance <= getAttackReachSqr(target) && isTimeToAttack() && host.mob().getMainHandItem().isEmpty()) host.action();
        super.checkAndPerformAttack(target, distance);
    }
}
