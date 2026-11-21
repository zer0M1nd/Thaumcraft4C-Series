package tc4revival.content;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public  class EldritchOrbEntity extends ThrowableProjectile {
    public EldritchOrbEntity(EntityType<? extends EldritchOrbEntity> type,Level level) { super(type,level); }
    public EldritchOrbEntity(Level level,LivingEntity owner) {
        this(TCEntities.ELDRITCH_ORB.get(),level); EldritchProjectileLaunch.initialize(this,owner);
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return 0; }
    @Override public void shoot(double x,double y,double z,float speed,float spread) { EldritchProjectileLaunch.shoot(this,random,x,y,z,speed,spread); }
    @Override public void tick() { super.tick(); if (tickCount>100) discard(); }
    @Override public void handleEntityEvent(byte event) {
        if (event==16) { if (level().isClientSide) tc4revival.client.EldritchWisps.impact(this); }
        else super.handleEntityEvent(event);
    }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || !(getOwner() instanceof LivingEntity owner) || tickCount>=100) return;
        var attribute=owner.getAttribute(Attributes.ATTACK_DAMAGE);
        float damage=attribute==null ? 0 : (float)attribute.getValue()*.666f;
        for (var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2))) {
            if (target==owner || target.getMobType()==MobType.UNDEAD) continue;
            if(target.hurt(impactDamage(owner),damage))target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,160,0));
        }
        playSound(SoundEvents.FIRE_EXTINGUISH,.5f,2.6f+(random.nextFloat()-random.nextFloat())*.8f);
        tickCount=100; level().broadcastEntityEvent(this,(byte)16);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    public net.minecraft.world.damagesource.DamageSource impactDamage(LivingEntity owner) {
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:eldritch_orb"));
        return new net.minecraft.world.damagesource.DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key),this,owner) {
            @Override public boolean is(net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> tag) {
                // Vanilla's bypasses_shield includes the entire bypasses_armor tag.
                // Retain magic armor bypass per hit without making the type unblockable.
                return tag.equals(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)||super.is(tag);
            }
        };
    }
}
