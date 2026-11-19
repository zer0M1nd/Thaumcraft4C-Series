package conciliummodern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
/** Shared legacy hostile target policy. */
public abstract class ConciliumHostile extends Monster {
 protected ConciliumHostile(EntityType<? extends Monster> type,Level level){super(type,level);setCanPickUpLoot(false);}
 protected void basicGoals(double speed,boolean leap){goalSelector.addGoal(0,new FloatGoal(this));if(leap)goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63f));goalSelector.addGoal(3,new MeleeAttackGoal(this,speed,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,speed));goalSelector.addGoal(6,new RandomStrollGoal(this,speed));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targets();}
 protected void targets(){legacyTargets(true,false,true,false);}
 protected void legacyTargets(boolean retaliate,boolean alert,boolean sight,boolean mad){if(retaliate){var hurt=new HurtByTargetGoal(this);if(alert)hurt.setAlertOthers();targetSelector.addGoal(1,hurt);}targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));int priority=3;if(mad)targetSelector.addGoal(priority++,new NearestAttackableTargetGoal<>(this,MadThaumaturge.class,true));targetSelector.addGoal(priority++,new NearestAttackableTargetGoal<>(this,Thaumaturge.class,sight));targetSelector.addGoal(priority,new NearestAttackableTargetGoal<>(this,tc4revival.content.CultistEntity.class,sight));}
}
