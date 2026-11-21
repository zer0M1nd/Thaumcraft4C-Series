package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Plan the tree bases before departure; chop each connected tree from its base. */
public  class GolemLumberGoal extends Goal {
    private static final GameProfile PROFILE=new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(StandardCharsets.UTF_8)),"FakeThaumcraftGolem");
    private final GolemWorkHost host;
    private final int radius;
    private final GolemTaskRoute route;
    private BlockPos target;
    private BlockState expected;
    private int ticks,delay=-1,maxDelay=1,step=1;
    public GolemLumberGoal(GolemWorkHost host) {
        this.host=host; route=new GolemTaskRoute(host);radius=(int)Math.ceil(host.configuration().range()/3f);
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    private boolean log(BlockPos pos) {
        var level=host.mob().level();
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS);
    }
    private void select(BlockPos pos) { target=pos; expected=host.mob().level().getBlockState(pos); }
    @Override public boolean canUse() {
        if (!host.workEnabled() || delay>=0 || host.mob().tickCount%Math.max(1,host.workDelay())!=0 || !host.mob().getNavigation().isDone()) return false;
        var home=host.home();var stops=new java.util.ArrayList<BlockPos>();
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=-3;y<=3;y++) {
            var pos=home.offset(x,y,z);
            if(log(pos) && !log(pos.below()))stops.add(pos);
        }
        if(!route.plan(stops))return false;
        select(route.target());return true;
    }
    @Override public void start() {
        ticks=1200;route.start();
    }
    @Override public boolean canContinueToUse() {
        return host.workEnabled() && target!=null && !route.empty() && ticks>0;
    }
    private void next() {
        if(target!=null)host.mob().level().destroyBlockProgress(host.mob().getId(),target,-1);
        delay=-1;route.advance();target=route.target();if(target!=null)select(target);
    }
    @Override public void tick() {
        if (!(host.mob().level() instanceof ServerLevel level) || target==null) return;
        ticks--;
        if(!level.hasChunkAt(target) || !level.getBlockState(target).equals(expected) || !log(target)){next();return;}
        host.mob().getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
        if (!GolemWorkRange.reaches(host,target)) {if(host.mob().getNavigation().isDone())next();return;}
        if (delay<0) {
            delay=maxDelay=(int)Math.max(5,(20-host.configuration().strength()*3f)*expected.getDestroySpeed(level,target));
            step=delay/Math.round(delay/6f);
        }
        if (delay<=0) return;
        if (--delay>0 && delay%step==0 && host.mob().getNavigation().isDone()) {
            host.action();
            var sound=expected.getSoundType(level,target,host.mob());
            level.playSound(null,target,sound.getBreakSound(),SoundSource.BLOCKS,(sound.getVolume()+.7f)/8,sound.getPitch()*.5f);
            level.destroyBlockProgress(host.mob().getId(),target,(int)(9*(1-delay/(float)maxDelay)));
        }
        if (delay!=0) return;
        level.levelEvent(2001,target,Block.getId(expected));
        var furthest=ElementalTreeSearch.furthest(level,target);
        var player=FakePlayerFactory.get(level,PROFILE);
        if (ElementalHarvest.harvest(player,furthest,false,0)) {
            host.transport().progress();ticks=1200;
            for (int x=-3;x<=3;x++) for (int y=-3;y<=3;y++) for (int z=-3;z<=3;z++) {
                var update=furthest.offset(x,y,z); int due=150+level.random.nextInt(150);
                if (!level.hasChunkAt(update) || level.isOutsideBuildHeight(update)) continue;
                var state=level.getBlockState(update);
                if (state.isAir()) continue;
                level.scheduleTick(update,state.getBlock(),due);
                if (state.isRandomlyTicking()) LampPlantGrowth.schedule(level,update,state,due);
            }
        }
        host.action();
        if (log(target)) { delay=-1; select(target);return; }
        next();
    }
    @Override public void stop() {
        if (target!=null) host.mob().level().destroyBlockProgress(host.mob().getId(),target,-1);
        delay=-1;route.clear();host.mob().getNavigation().stop();
    }
}
