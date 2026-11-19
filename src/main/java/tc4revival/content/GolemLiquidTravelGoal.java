package tc4revival.content;

import net.minecraft.core.BlockPos;

public class GolemLiquidTravelGoal extends GolemMoveToGoal {
    public GolemLiquidTravelGoal(GolemWorkHost host) { super(host); }
    @Override protected BlockPos destination() {
        if (host.fluid().getAmount() > host.configuration().fluidCarryLimit() - 1000) return null;
        for (var fluid : GolemLiquidSources.needed(host)) {
            var source = GolemLiquidSources.find(host, fluid);
            if (source != null) { host.watchingFluid(true); return source; }
        }
        return null;
    }
}
