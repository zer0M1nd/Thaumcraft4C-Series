package advancedthaumaturgy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
public class SentryFireball extends SmallFireball {
    public SentryFireball(EntityType<? extends SmallFireball> type,Level level){super(type,level);}
    @Override protected void onHitBlock(BlockHitResult hit){if(!level().isClientSide)discard();}
    @Override protected void onHitEntity(EntityHitResult hit){if(!level().isClientSide){var target=hit.getEntity();if(!target.fireImmune()&&target.hurt(damageSources().fireball(this,getOwner()),5))target.setSecondsOnFire(5);discard();}}
}
