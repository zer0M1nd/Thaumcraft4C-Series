package tc4revival.content;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Small spiders released by taint spores; they acquire players even in daylight. */
public  class TaintSpiderEntity extends Spider implements TaintedMob {
    public TaintSpiderEntity(EntityType<? extends TaintSpiderEntity> type,Level level) {
        super(type,level); xpReward=2;
    }
    public static AttributeSupplier.Builder attributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,2)
                .add(Attributes.FOLLOW_RANGE,12);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        // The old override replaced Spider's light-gated player lookup. No golem targeting.
        targetSelector.removeAllGoals(goal->true);
        targetSelector.addGoal(1,new TargetGoal(this,false) {
            private Player selected;
            @Override public boolean canUse() {
                selected=null; double nearest=144;
                for (var player:level().players()) {
                    if (!player.isAlive() || player.isSpectator() || player.getAbilities().instabuild) continue;
                    double distance=distanceToSqr(player);
                    if (distance<nearest) { nearest=distance; selected=player; }
                }
                return selected!=null;
            }
            @Override public void start() { setTarget(selected); super.start(); }
        });
    }
    @Override public float getVoicePitch() { return .7f; }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
}
