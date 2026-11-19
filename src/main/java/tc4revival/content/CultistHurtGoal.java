package tc4revival.content;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.phys.AABB;

public  class CultistHurtGoal extends HurtByTargetGoal {
    private final PathfinderMob cultist;
    public CultistHurtGoal(PathfinderMob cultist) { super(cultist); this.cultist=cultist; setAlertOthers(); }
    @Override protected void alertOthers() {
        var attacker=cultist.getLastHurtByMob(); if (attacker==null) return;
        double range=cultist.getAttributeValue(Attributes.FOLLOW_RANGE);
        var area=new AABB(cultist.getX(),cultist.getY(),cultist.getZ(),cultist.getX()+1,cultist.getY()+1,cultist.getZ()+1).inflate(range,10,range);
        for (var ally:cultist.level().getEntitiesOfClass(CultistEntity.class,area)) {
            if (ally==cultist || ally.getTarget()!=null || ally.isAlliedTo(attacker)) continue;
            if (ally instanceof CultistClericEntity cleric && cleric.ritualist()) {
                if (cultist.getRandom().nextInt(3)!=0) continue; cleric.setRitualist(false);
            }
            ally.setTarget(attacker);
        }
    }
}
