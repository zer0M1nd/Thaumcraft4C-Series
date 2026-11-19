package tmmodern;
import net.minecraft.world.entity.*;import net.minecraft.world.level.Level;import net.minecraft.world.phys.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.effect.*;
public class DarkMatterProjectile extends tc4revival.content.EldritchOrbEntity {
    public float damage=16,radius=1.5f;public boolean corrosive;
    public DarkMatterProjectile(EntityType<? extends DarkMatterProjectile> type,Level level){super(type,level);}
    public boolean diffusion(){return getType()==TaintedMagic.DIFFUSION.get();}
    @Override public void handleEntityEvent(byte event){if(diffusion()&&event==16){if(level().isClientSide)TMClient.diffusionImpact(this);}else super.handleEntityEvent(event);}
    @Override public void tick(){super.tick();if(diffusion()){if(tickCount>=20)discard();setDeltaMovement(getDeltaMovement().scale(onGround()?.95*.66:.95));}}
    @Override protected void onHit(HitResult hit){if(level().isClientSide||!(getOwner() instanceof LivingEntity owner))return;if(!diffusion()||hit instanceof EntityHitResult){for(var e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius))){if(e==owner)continue;e.hurt(impactDamage(owner),damage);e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,diffusion()?40:160,1));if(corrosive)e.addEffect(new MobEffectInstance(MobEffects.WITHER,diffusion()?40:160,1));}}if(!diffusion())playSound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,.5f,2.6f+(random.nextFloat()-random.nextFloat())*.8f);level().broadcastEntityEvent(this,(byte)16);discard();}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putFloat("Damage",damage);t.putFloat("Radius",radius);t.putBoolean("Corrosive",corrosive);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);damage=t.getFloat("Damage");radius=t.getFloat("Radius");corrosive=t.getBoolean("Corrosive");}
}
