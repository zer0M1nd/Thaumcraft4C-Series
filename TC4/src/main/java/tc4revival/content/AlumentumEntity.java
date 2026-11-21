package tc4revival.content;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public  class AlumentumEntity extends ThrowableProjectile {
    public AlumentumEntity(EntityType<? extends AlumentumEntity> type,Level level) { super(type,level); }
    public AlumentumEntity(Level level,LivingEntity owner) {
        this(TCEntities.ALUMENTUM.get(),level); setOwner(owner);
        float yaw=owner.getYRot()*net.minecraft.util.Mth.DEG_TO_RAD;
        setPos(owner.getX()-net.minecraft.util.Mth.cos(yaw)*.16,owner.getEyeY()-.1,owner.getZ()-net.minecraft.util.Mth.sin(yaw)*.16);
        setYRot(owner.getYRot()); setXRot(owner.getXRot()); var look=owner.getLookAngle(); shoot(look.x,look.y,look.z,.75f,1);
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return .03f; }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || isRemoved()) return;
        boolean destroy=level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        // Preserve the non-incendiary charge and configured terrain damage while crediting the thrower.
        level().explode(this,getX(),getY(),getZ(),1.66f,false,destroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE); discard();
    }
    @Override public void tick() { super.tick(); if (level().isClientSide) tc4revival.client.FocusProjectileEffects.alumentum(this); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
}
