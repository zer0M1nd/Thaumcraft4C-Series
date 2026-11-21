package thmodern.vat;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;

/** Loyalty is installed on the actual mob, including non-PathfinderMob subclasses. */
public final class LoyalCreatureGoals {
    private static final Set<Mob> INSTALLED=Collections.newSetFromMap(new WeakHashMap<>());
    public static Player owner(Mob mob){var data=mob.getPersistentData().getCompound(CreatureInfusions.DATA);return data.hasUUID("Owner")?mob.level().getPlayerByUUID(data.getUUID("Owner")):null;}
    public static boolean sitting(Mob mob){return mob.getPersistentData().getCompound(CreatureInfusions.DATA).getBoolean("Sitting");}
    public static boolean ally(Mob mob,LivingEntity target){
        if(target==mob||target==owner(mob)||mob.isAlliedTo(target))return true;
        var data=mob.getPersistentData().getCompound(CreatureInfusions.DATA);var other=target.getPersistentData().getCompound(CreatureInfusions.DATA);
        return data.hasUUID("Owner")&&other.hasUUID("Owner")&&data.getUUID("Owner").equals(other.getUUID("Owner"));
    }
    private static boolean enemy(Mob mob,LivingEntity target){return target!=null&&target.isAlive()&&!ally(mob,target)&&(!(target instanceof Player player)||!player.isCreative()&&!player.isSpectator());}
    public static void install(Mob mob){
        if(!INSTALLED.add(mob))return;
        mob.goalSelector.removeAllGoals(goal->true);mob.targetSelector.removeAllGoals(goal->true);mob.setTarget(null);
        mob.goalSelector.addGoal(1,new FloatGoal(mob));mob.goalSelector.addGoal(2,new Sit(mob));mob.goalSelector.addGoal(3,new LeapAtTargetGoal(mob,.4f));
        mob.goalSelector.addGoal(4,new Attack(mob));mob.goalSelector.addGoal(5,new Follow(mob));
        if(mob instanceof net.minecraft.world.entity.animal.Animal animal)mob.goalSelector.addGoal(6,new BreedGoal(animal,1));
        if(mob instanceof PathfinderMob pathfinder)mob.goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(pathfinder,1));
        mob.goalSelector.addGoal(9,new LookAtPlayerGoal(mob,Player.class,8));mob.goalSelector.addGoal(9,new RandomLookAroundGoal(mob));mob.targetSelector.addGoal(1,new Defend(mob));
    }
    private static final class Sit extends Goal {
        private final Mob mob;Sit(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
        @Override public boolean canUse(){return sitting(mob);}
        @Override public void start(){mob.getNavigation().stop();mob.setTarget(null);}
        @Override public void tick(){mob.getNavigation().stop();}
    }
    private static final class Follow extends Goal {
        private final Mob mob;private Player owner;private int delay;
        Follow(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){owner=owner(mob);return owner!=null&&!sitting(mob)&&!mob.isPassenger()&&mob.distanceToSqr(owner)>=100;}
        @Override public boolean canContinueToUse(){return owner!=null&&owner.isAlive()&&!sitting(mob)&&!mob.isPassenger()&&mob.distanceToSqr(owner)>4;}
        @Override public void stop(){owner=null;mob.getNavigation().stop();}
        @Override public void start(){delay=0;}
        @Override public void tick(){
            mob.getLookControl().setLookAt(owner,10,mob.getMaxHeadXRot());if(--delay>0)return;delay=10;
            if(mob.getNavigation().moveTo(owner,1)||mob.isLeashed()||mob.distanceToSqr(owner)<144)return;
            var level=mob.level();var center=owner.blockPosition();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2||Math.abs(z)==2){
                var pos=center.offset(x,0,z);if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;
                var destination=net.minecraft.world.phys.Vec3.atBottomCenterOf(pos);var moved=mob.getBoundingBox().move(destination.subtract(mob.position()));
                if(level.noCollision(mob,moved)&&!level.containsAnyLiquid(moved)){mob.moveTo(destination);mob.getNavigation().stop();return;}
            }
        }
    }
    private static final class Attack extends Goal {
        private final Mob mob;private int cooldown;
        Attack(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return !sitting(mob)&&enemy(mob,mob.getTarget());}
        @Override public void stop(){mob.getNavigation().stop();}
        @Override public void tick(){
            var target=mob.getTarget();if(!enemy(mob,target))return;mob.getLookControl().setLookAt(target,30,30);mob.getNavigation().moveTo(target,1);
            if(cooldown>0)cooldown--;
            double reach=mob.getBbWidth()*2; if(cooldown==0&&mob.distanceToSqr(target)<=reach*reach+target.getBbWidth()&&mob.hasLineOfSight(target)){
                cooldown=20;mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);var attack=mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                target.hurt(mob.damageSources().mobAttack(mob),3+(attack==null?0:(float)attack.getValue()));
            }
        }
    }
    private static final class Defend extends Goal {
        private final Mob mob;private int ownerHurt=-1,ownerAttack=-1,selfHurt=-1;private LivingEntity target;
        Defend(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            target=null;if(sitting(mob))return false;var player=owner(mob);
            if(player!=null&&player.getLastHurtByMobTimestamp()!=ownerHurt){ownerHurt=player.getLastHurtByMobTimestamp();if(enemy(mob,player.getLastHurtByMob()))target=player.getLastHurtByMob();}
            if(target==null&&player!=null&&player.getLastHurtMobTimestamp()!=ownerAttack){ownerAttack=player.getLastHurtMobTimestamp();if(enemy(mob,player.getLastHurtMob()))target=player.getLastHurtMob();}
            if(target==null&&mob.getLastHurtByMobTimestamp()!=selfHurt){selfHurt=mob.getLastHurtByMobTimestamp();if(enemy(mob,mob.getLastHurtByMob()))target=mob.getLastHurtByMob();}
            return target!=null;
        }
        @Override public boolean canContinueToUse(){return false;}
        @Override public void start(){
            mob.setTarget(target);
            if(target==mob.getLastHurtByMob()){
                double range=mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)==null?16:mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                for(var other:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(range,10,range)))
                    if(other!=mob&&other.getClass()==mob.getClass()&&other.getTarget()==null&&enemy(other,target))other.setTarget(target);
            }
        }
    }
}
