package tc4revival.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Native melee scheduling with the tainted creature's target-class restriction. */
class TaintMeleeGoal extends MeleeAttackGoal {
    private final Class<? extends LivingEntity> targetClass;
    TaintMeleeGoal(PathfinderMob mob,Class<? extends LivingEntity> targetClass,boolean memory) {
        super(mob,1,memory);this.targetClass=targetClass;
    }
    @Override public boolean canUse() { return targetClass.isInstance(mob.getTarget()) && super.canUse(); }
    @Override public boolean canContinueToUse() { return targetClass.isInstance(mob.getTarget()) && super.canContinueToUse(); }
}
