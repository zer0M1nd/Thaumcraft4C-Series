package gadomancymodern.golem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import tc4revival.content.GolemEntity;

public class BodyguardGoals {
    public static class Sit extends Goal {
        protected final GolemEntity golem;public Sit(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
        public boolean canUse(){return GadGolems.sitting(golem);}public boolean canContinueToUse(){return canUse();}public void start(){golem.getNavigation().stop();}public void tick(){golem.getLookControl().setLookAt(0,0,0,0,-45);}
    }
    public static class OwnerTarget extends Goal {
        protected final GolemEntity golem;protected final boolean hurtBy;protected LivingEntity target;protected int timestamp;
        public OwnerTarget(GolemEntity golem,boolean hurtBy){this.golem=golem;this.hurtBy=hurtBy;setFlags(java.util.EnumSet.of(Flag.TARGET));}
        public boolean canUse(){var owner=GadGolems.owner(golem);if(owner==null)return false;target=hurtBy?owner.getLastHurtByMob():owner.getLastHurtMob();int time=hurtBy?owner.getLastHurtByMobTimestamp():owner.getLastHurtMobTimestamp();return time!=timestamp&&target!=null&&target.isAlive()&&target!=golem&&golem.canAttack(target)&&golem.canAttackType(target.getType())&&!(target instanceof net.minecraft.world.entity.TamableAnimal tame&&tame.isOwnedBy(owner));}
        public void start(){golem.setTarget(target);var owner=GadGolems.owner(golem);if(owner!=null)timestamp=hurtBy?owner.getLastHurtByMobTimestamp():owner.getLastHurtMobTimestamp();}public boolean canContinueToUse(){return false;}
    }
    public static class Follow extends Goal {
        protected final GolemEntity golem;protected net.minecraft.world.entity.player.Player owner;protected int pathTicks;
        public Follow(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean canUse(){owner=GadGolems.owner(golem);return owner!=null&&!GadGolems.sitting(golem)&&golem.distanceToSqr(owner)>=25;}
        public boolean canContinueToUse(){return owner!=null&&owner.isAlive()&&!GadGolems.sitting(golem)&&!golem.getNavigation().isDone()&&golem.distanceToSqr(owner)>4;}
        public void start(){pathTicks=0;}public void stop(){owner=null;golem.getNavigation().stop();}
        public boolean requiresUpdateEveryTick(){return true;}
        public void tick(){golem.getLookControl().setLookAt(owner,10,golem.getMaxHeadXRot());if(--pathTicks>0)return;pathTicks=10;if(golem.getNavigation().moveTo(owner,golem.navigationSpeed())||golem.isLeashed()||golem.distanceToSqr(owner)<144)return;var origin=owner.blockPosition();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){if(Math.abs(x)<2&&Math.abs(z)<2)continue;var pos=origin.offset(x,0,z);if(golem.level().hasChunkAt(pos)&&golem.level().getBlockState(pos.below()).isFaceSturdy(golem.level(),pos.below(),net.minecraft.core.Direction.UP)&&golem.level().isEmptyBlock(pos)&&golem.level().isEmptyBlock(pos.above())){golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,golem.getYRot(),golem.getXRot());golem.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,.5f,1);golem.getNavigation().stop();return;}}}
    }
    /** TC4 collision timing, deliberately without guard-core marker/creeper category filters. */
    public static class Melee extends Goal {
        protected final GolemEntity golem;protected LivingEntity target;protected net.minecraft.world.level.pathfinder.Path path;protected int attack,counter;
        public Melee(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean canUse(){target=golem.getTarget();if(!golem.workEnabled()||target==null||!target.isAlive())return false;path=golem.getNavigation().createPath(target,0);return path!=null;}
        public boolean canContinueToUse(){return canUse()&&!golem.getNavigation().isDone();}public void start(){golem.getNavigation().moveTo(path,golem.navigationSpeed());counter=0;}public void stop(){target=null;golem.getNavigation().stop();}public boolean requiresUpdateEveryTick(){return true;}
        public void tick(){golem.getLookControl().setLookAt(target,30,30);if(golem.getSensing().hasLineOfSight(target)&&--counter<=0){counter=4+golem.getRandom().nextInt(7);golem.getNavigation().moveTo(target,golem.navigationSpeed());}attack=Math.max(attack-1,0);double reach=target.getBbWidth()*2f*target.getBbWidth()*2f+1;if(golem.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=reach&&attack<=0){attack=20-(golem.configuration().advanced()?2:0);golem.action();golem.doHurtTarget(target);}}
    }
    public static class Dart extends Goal {
        protected final GolemEntity golem;protected final int interval;protected int remaining;protected LivingEntity target;
        public Dart(GolemEntity golem){this.golem=golem;interval=30-golem.configuration().upgrades(0)*8;remaining=interval/2;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean canUse(){target=golem.getTarget();return golem.workEnabled()&&target!=null&&target.isAlive()&&golem.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())>=9;}public boolean canContinueToUse(){return canUse()&&!golem.getNavigation().isDone();}public void stop(){target=null;remaining=interval/2;}public boolean requiresUpdateEveryTick(){return true;}
        public void tick(){golem.getNavigation().moveTo(target,golem.navigationSpeed());if(!golem.getSensing().hasLineOfSight(target))return;golem.getLookControl().setLookAt(target,30,30);remaining=Math.max(remaining-1,0);float range=golem.configuration().range()*.8f;if(remaining<=0&&golem.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=range*range){golem.dart(target);remaining=interval;}}
    }
}
