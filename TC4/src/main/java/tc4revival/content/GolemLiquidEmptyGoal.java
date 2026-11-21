package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.fluids.capability.IFluidHandler;

public  class GolemLiquidEmptyGoal extends Goal {
    private final GolemWorkHost host;
    public GolemLiquidEmptyGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean canUse() {
        return host.workEnabled() && !host.fluid().isEmpty() && GolemWorkRange.reaches(host,host.home(),5) && GolemLiquidSources.needed(host).contains(host.fluid().getFluid());
    }
    @Override public boolean canContinueToUse() { return false; }
    @Override public void start() {
        if (!(host.mob().level() instanceof ServerLevel level)) return;
        host.mob().getNavigation().stop();
        var pos=host.homeInventory(); var handler=GolemLiquidSources.at(level,pos,host.homeFacing());
        if (handler==null) return;
        var remaining=host.fluid().copy(); int filled=handler.fill(remaining,IFluidHandler.FluidAction.EXECUTE);
        remaining.shrink(filled); host.fluid(remaining); host.watchingFluid(false);
        if (filled>200) level.playSound(null,host.mob(),SoundEvents.GENERIC_SWIM,SoundSource.NEUTRAL,
                Math.min(.2f,.2f*filled/host.configuration().fluidCarryLimit()),1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),2);
    }
}
