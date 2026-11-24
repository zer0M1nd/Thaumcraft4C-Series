package tc4revival.content;

import net.minecraft.core.BlockPos;

/** Return to the actual work point using native move-to-block scheduling. */
public class GolemReturnHomeGoal extends GolemMoveToGoal {
    public GolemReturnHomeGoal(GolemWorkHost host) { super(host); }
    @Override protected BlockPos destination() { return host.home(); }
    @Override protected boolean arrived() {
        return GolemWorkRange.reaches(host, host.home(), 5) && (host.configuration().core() != GolemCoreType.ESSENTIA || host.essentiaAmount() > 0
                || GolemWorkRange.reaches(host, host.homeInventory(), 6));
    }
}
