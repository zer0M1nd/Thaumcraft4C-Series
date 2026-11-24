package tc4revival.content;

import java.util.ArrayList;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Harvest a departure-time itinerary using the material strength for each work step. */
public  class GolemHarvestGoal extends Goal {
    private final GolemWorkHost host;
    private final int radius;
    private final GolemTaskRoute route;
    private BlockPos target;
    private BlockState expected;
    private int ticks,delay=-1,maxDelay=1,step=1;
    public GolemHarvestGoal(GolemWorkHost host) {
        this.host=host; route=new GolemTaskRoute(host); radius=(int)Math.ceil(host.configuration().range()/4f);
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    private void select(BlockPos pos) { target=pos; expected=host.mob().level().getBlockState(pos); }
    @Override public boolean canUse() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || delay>=0
                || host.mob().tickCount%Math.max(1,host.workDelay())!=0 || !host.mob().getNavigation().isDone()) return false;
        var home=host.home(); var stops=new ArrayList<BlockPos>();
        for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) for(int y=-3;y<=3;y++) {
            var pos=home.offset(x,y,z);
            if(level.hasChunkAt(pos) && GolemCropHarvest.mature(level,pos)) stops.add(pos);
        }
        if(!route.plan(stops,0,this::approaches)) return false;
        select(route.target());return true;
    }
    private java.util.Set<BlockPos> approaches(BlockPos crop) {
        // Walk up beside a cactus or underneath a fruiting vine. The harvested block
        // remains the route destination for permissions, drops and animation.
        var positions=new java.util.HashSet<BlockPos>();
        double reach=2+tc4revival.config.TCConfig.SERVER.golemInteractionBonus.get();
        for(int y=-2;y<=1;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            var feet=crop.offset(x,y,z);
            if (Vec3.atBottomCenterOf(feet).distanceToSqr(Vec3.atCenterOf(crop))<=reach*reach)
                positions.add(feet);
        }
        return positions;
    }
    @Override public void start() { ticks=1200; route.start(); }
    @Override public boolean canContinueToUse() { return host.workEnabled() && !route.empty() && ticks>0; }
    private void next() {
        if(target!=null)host.mob().level().destroyBlockProgress(host.mob().getId(),target,-1);
        delay=-1;route.advance();target=route.target();if(target!=null)select(target);
    }
    @Override public void tick() {
        if (!(host.mob().level() instanceof ServerLevel level) || target==null) return;
        ticks--;
        if(!level.hasChunkAt(target) || !level.getBlockState(target).equals(expected) || !GolemCropHarvest.mature(level,target)) { next();return; }
        host.mob().getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
        if (!GolemWorkRange.reaches(host,target)) { if(host.mob().getNavigation().isDone())next();return; }
        if (delay<0) {
            delay=maxDelay=(int)Math.max(10,(20-host.configuration().strength()*2f)*expected.getDestroySpeed(level,target));
            step=delay/Math.round(delay/6f);
        }
        if (delay<=0) return;
        if (--delay>0 && delay%step==0 && host.mob().getNavigation().isDone()) {
            host.action(); var sound=expected.getSoundType(level,target,host.mob());
            level.playSound(null,target,sound.getBreakSound(),SoundSource.BLOCKS,(sound.getVolume()+.7f)/8,sound.getPitch()*.5f);
            level.destroyBlockProgress(host.mob().getId(),target,(int)(9*(1-delay/(float)maxDelay)));
        }
        if (delay!=0) return;
        ticks=1200; GolemCropHarvest.harvest(host,target,expected);
        if (!level.getBlockState(target).equals(expected)) host.transport().progress();
        next();
    }
    @Override public void stop() {
        if (target!=null) host.mob().level().destroyBlockProgress(host.mob().getId(),target,-1);
        delay=-1;route.clear();host.mob().getNavigation().stop();
    }
}
