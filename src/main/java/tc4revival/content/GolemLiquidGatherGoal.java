package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Pump every ten ticks while sources remain; direct tanks precede adjacent source blocks. */
public  class GolemLiquidGatherGoal extends Goal {
    private final GolemWorkHost host;
    private final GolemPumpQueue queue=new GolemPumpQueue();
    private int ticks;
    public GolemLiquidGatherGoal(GolemWorkHost host) { this.host=host; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || space()<=0) return false;
        for (var fluid:GolemLiquidSources.needed(host)) {
            for (var marker:GolemLiquidSources.handlers(host,fluid,true)) {
                var handler=GolemLiquidSources.at(level,marker.pos(),marker.face());
                if (handler!=null && !handler.drain(new FluidStack(fluid,space()),IFluidHandler.FluidAction.SIMULATE).isEmpty()) return true;
            }
            for (var pos:GolemLiquidSources.adjacentBlocks(host)) if (space()>=1000 && GolemLiquidSources.source(level,pos,fluid)) return true;
        }
        return false;
    }
    private int space() { return Math.max(0,host.configuration().fluidCarryLimit()-host.fluid().getAmount()); }
    @Override public void start() { ticks=0;host.mob().getNavigation().stop();host.watchingFluid(true); }
    @Override public boolean canContinueToUse() { return host.workEnabled() && ticks<20 && host.watchingFluid(); }
    @Override public void stop() { ticks=0; host.watchingFluid(false); }
    private void add(ServerLevel level,FluidStack extracted,boolean block) {
        var held=host.fluid().copy();
        if (held.isEmpty()) held=extracted.copy(); else held.grow(extracted.getAmount());
        host.fluid(held);
        if (block || extracted.getAmount()>200) level.playSound(null,host.mob(),SoundEvents.GENERIC_SWIM,SoundSource.NEUTRAL,
                block?.2f:.2f*extracted.getAmount()/host.configuration().fluidCarryLimit(),1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        if (block?held.getAmount()>host.configuration().fluidCarryLimit()-1000:held.getAmount()>=host.configuration().fluidCarryLimit()) host.watchingFluid(false);
        ticks=0;
    }
    @Override public void tick() {
        if (++ticks<10 || !(host.mob().level() instanceof ServerLevel level)) return;
        for (var fluid:GolemLiquidSources.needed(host)) {
            if (!host.fluid().isEmpty() && host.fluid().getFluid()!=fluid) continue;
            for (var marker:GolemLiquidSources.handlers(host,fluid,true)) {
                var handler=GolemLiquidSources.at(level,marker.pos(),marker.face());
                if (handler==null || space()==0) continue;
                var extracted=handler.drain(new FluidStack(fluid,space()),IFluidHandler.FluidAction.EXECUTE);
                if (!extracted.isEmpty()) add(level,extracted,false);
            }
            for (var marked:GolemLiquidSources.adjacentBlocks(host)) {
                if (!GolemLiquidSources.connected(level,marked,fluid) || space()<1000) continue;
                var source=host.configuration().upgrades(5)>0?queue.next(level,marked,fluid,host.configuration().range()):marked;
                if (source==null || !GolemLiquidSources.source(level,source,fluid)) continue;
                // Revalidate each queued source; old stale coordinates could generate fluid from air.
                if (GolemLiquidSources.takeSource(level,source,fluid)) add(level,new FluidStack(fluid,1000),true);
            }
        }
    }
}
