package tc4revival.content;

import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Native visible-target ranged combat; close targets are handed to melee. */
public class GolemDartGoal extends RangedAttackGoal {
    private final GolemWorkHost host;
    public GolemDartGoal(GolemWorkHost host) {
        super((RangedAttackMob) host.mob(), host.navigationSpeed(),
                Math.max(6, 30 - host.configuration().upgrades(0) * 8), host.configuration().range() * .8f);
        this.host = host;
    }
    @Override public boolean canUse() {
        var target = host.mob().getTarget();
        return host.workEnabled() && target != null && GolemCombatTargets.valid(host, target)
                && host.mob().distanceToSqr(target) >= 9 && super.canUse();
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void tick() {
        super.tick();
        host.mob().getNavigation().setSpeedModifier(host.navigationSpeed());
    }
}
