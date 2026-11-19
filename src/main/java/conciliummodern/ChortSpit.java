package conciliummodern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
public final class ChortSpit extends ThrowableItemProjectile {
 public ChortSpit(EntityType<? extends ChortSpit> type,Level level){super(type,level);}
 @Override protected Item getDefaultItem(){return Items.SLIME_BALL;}
 @Override protected float getGravity(){return .01f;}
 @Override protected boolean canHitEntity(Entity entity){var owner=getOwner();return super.canHitEntity(entity)&&entity!=owner&&(owner==null||entity!=owner.getVehicle());}
 @Override protected void onHitEntity(EntityHitResult hit){super.onHitEntity(hit);if(!level().isClientSide&&getOwner() instanceof LivingEntity owner&&hit.getEntity()!=owner.getVehicle()&&hit.getEntity()!=owner)hit.getEntity().hurt(damageSources().thrown(this,owner),(float)owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));}
 @Override protected void onHit(HitResult hit){super.onHit(hit);playSound(net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL,.5f,1);if(!level().isClientSide)discard();}
 @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount>200)discard();}
}
