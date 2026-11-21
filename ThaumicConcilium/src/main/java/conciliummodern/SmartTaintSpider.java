package conciliummodern;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import tc4revival.content.TaintSpiderEntity;

/** Animated spiders accept explicit targets without independently hunting the caster. */
public final class SmartTaintSpider extends TaintSpiderEntity {
    public SmartTaintSpider(EntityType<? extends SmartTaintSpider> type,Level level){super(type,level);}
    @Override protected void registerGoals(){
        super.registerGoals();targetSelector.removeAllGoals(goal->true);
        goalSelector.removeAllGoals(goal->goal instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal);
        goalSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this,1,true));
    }
}
