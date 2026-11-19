package tc4revival.content;

import net.minecraft.core.BlockPos;

public class GolemEssentiaTravelGoal extends GolemMoveToGoal {
    public GolemEssentiaTravelGoal(GolemWorkHost host) { super(host); }
    @Override protected BlockPos destination() {
        return host.essentia() == null || host.essentiaAmount() <= 0 ? null : GolemEssentiaTargets.find(host);
    }
}
