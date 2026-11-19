package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeType;

/** Non-player fishing with native waiting intervals and data pack fishing loot. */
public  class GolemFishGoal extends Goal {
    private final GolemWorkHost host;
    private final int radius;
    private BlockPos target;
    private GolemBobberEntity bobber;
    private int ticks,biteDelay;
    private float quality;
    public GolemFishGoal(GolemWorkHost host) { this.host=host; radius=(int)Math.ceil(host.configuration().range()/2f); setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || target!=null || ticks>0
                || host.mob().tickCount%Math.max(1,host.workDelay())!=0 || !host.mob().getNavigation().isDone()) return false;
        if (bobber!=null) bobber.discard();
        target=GolemFishingWater.find(host,radius); if (target==null) return false;
        quality=GolemFishingWater.quality(level,target).bite();
        return true;
    }
    @Override public void start() {
        var level=(ServerLevel)host.mob().level();
        bobber=TCEntities.GOLEM_BOBBER.get().create(level);
        if(bobber==null) {target=null;return;}
        bobber.cast(host.mob(),target);
        if(!level.addFreshEntity(bobber)) {target=null;bobber=null;return;}
        level.playSound(null,host.mob(),SoundEvents.FISHING_BOBBER_THROW,SoundSource.NEUTRAL,.5f,1);
        // FishingHook is player-owned and cannot be used as a mob's live hook. Use its
        // 100..600-tick waiting interval with golem strength and pool quality reducing the delay.
        biteDelay=Math.max(40,100+level.random.nextInt(501)-host.configuration().strength()*15-(int)(quality*50000));
        ticks=1200;host.rightArm();
    }
    @Override public boolean canContinueToUse() { return host.workEnabled() && bobber!=null && bobber.isAlive() && target!=null && ticks>0; }
    @Override public void tick() {
        if (target==null || !(host.mob().level() instanceof ServerLevel level)) return;
        host.mob().getLookControl().setLookAt(target.getX()+.5,target.getY()+1,target.getZ()+.5,30,30);
        var config=host.configuration();ticks--;
        if(!bobber.isInWater())return;
        int step=1;
        if(level.random.nextFloat()<.25f && level.isRainingAt(target.above()))step++;
        if(level.random.nextFloat()<.5f && !level.canSeeSky(target.above()))step--;
        if((biteDelay-=step)>0)return;
        host.rightArm(); int count=1;
        if (config.upgrades(0)>0 && level.random.nextInt(10)<config.upgrades(0)) count++;
        for (int index=0;index<count;index++) {
            float roll=level.random.nextFloat();
            var pool=GolemFishingWater.pool(roll,config,GolemFishingWater.quality(level,target));
            var loot=GolemFishingLoot.nativeLoot(level,pool,bobber,config.upgrades(4));
            for(var stack:loot) {
            if (config.upgrades(2)>0) {
                var inventory=new SimpleContainer(stack);
                var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,inventory,level);
                if (recipe.isPresent()) stack=recipe.get().assemble(inventory,level.registryAccess());
            }
            var item=new ItemEntity(level,target.getX()+.5,target.getY()+1,target.getZ()+.5,stack);
            if (config.upgrades(2)>0) item.setSecondsOnFire(2);
            item.setPickUpDelay(20);
            double x=host.mob().getX()+level.random.nextFloat()-level.random.nextFloat()-target.getX()+.5;
            double y=host.mob().getY()-target.getY()+1;
            double z=host.mob().getZ()+level.random.nextFloat()-level.random.nextFloat()-target.getZ()+.5;
            double distance=(float)Math.sqrt(x*x+y*y+z*z);
            item.setDeltaMovement(x*.1,y*.1+(float)Math.sqrt(distance)*.08,z*.1);
            level.addFreshEntity(item);
            }
        }
        if (bobber!=null) {
            bobber.playSound(SoundEvents.GENERIC_SPLASH,.15f,1+(level.random.nextFloat()-level.random.nextFloat())*.4f);
            level.sendParticles(ParticleTypes.SPLASH,bobber.getX(),bobber.getY()+.5,bobber.getZ(),20+level.random.nextInt(20),.1f,0,.1f,0);
            bobber.discard();
        }
        target=null;
    }
    @Override public void stop() { if (bobber!=null) bobber.discard(); target=null; ticks=-1; }
}
