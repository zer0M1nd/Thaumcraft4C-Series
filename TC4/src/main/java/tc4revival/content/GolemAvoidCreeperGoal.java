package tc4revival.content;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;

/** Select the nearest visible primed creeper using native targeting and escape navigation. */
public class GolemAvoidCreeperGoal extends AvoidEntityGoal<Creeper> {
    private final GolemWorkHost host;
    public GolemAvoidCreeperGoal(GolemWorkHost host) {
        super(host.mob(), Creeper.class, 5, host.navigationSpeed() * 1.125,
                host.navigationSpeed() * 1.25,
                entity -> entity instanceof Creeper creeper && (creeper.getSwellDir() > 0 || creeper.isIgnited()));
        this.host = host;
    }
    @Override public boolean canUse() { return host.workEnabled() && super.canUse(); }
    @Override public boolean canContinueToUse() {
        return host.workEnabled() && toAvoid != null && toAvoid.isAlive() && super.canContinueToUse();
    }
    @Override public void tick() {
        super.tick();
        host.mob().getNavigation().setSpeedModifier(host.navigationSpeed() * (host.mob().distanceToSqr(toAvoid) < 49 ? 1.25 : 1.125));
    }
}
