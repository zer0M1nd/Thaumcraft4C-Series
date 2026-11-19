package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

/** Work destinations share vanilla retry scheduling and navigation stuck detection. */
public abstract class GolemMoveToGoal extends MoveToBlockGoal {
    protected final GolemWorkHost host;
    protected GolemMoveToGoal(GolemWorkHost host) {
        super(host.mob(), host.navigationSpeed(), 0);
        this.host = host;
    }
    protected abstract BlockPos destination();
    protected boolean arrived() { return blockPos.closerToCenterThan(mob.position(), acceptedDistance()); }
    @Override public double acceptedDistance() { return 1.5+tc4revival.config.TCConfig.SERVER.golemInteractionBonus.get(); }
    @Override protected int nextStartTick(PathfinderMob mob) { return Math.max(1, host.workDelay()); }
    @Override protected BlockPos getMoveToTarget() { return blockPos; }
    @Override protected boolean findNearestBlock() {
        var target = destination();
        if (target == null || !mob.level().hasChunkAt(target)) return false;
        blockPos = target.immutable();
        return !arrived();
    }
    @Override protected boolean isValidTarget(LevelReader level, BlockPos pos) { return level.hasChunkAt(pos); }
    @Override public boolean canUse() { return host.workEnabled() && super.canUse(); }
    @Override public boolean canContinueToUse() {
        return host.workEnabled() && !arrived() && super.canContinueToUse();
    }
    @Override protected void moveMobToBlock() {
        mob.getNavigation().moveTo(mob.getNavigation().createPath(blockPos, 0), host.navigationSpeed());
    }
    @Override public void tick() {
        super.tick();
        if (mob.getNavigation().isDone() && shouldRecalculatePath()) moveMobToBlock();
        mob.getNavigation().setSpeedModifier(host.navigationSpeed());
    }
    @Override public void stop() { mob.getNavigation().stop(); }
}
