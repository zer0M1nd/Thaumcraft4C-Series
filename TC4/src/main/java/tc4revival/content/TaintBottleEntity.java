package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public  class TaintBottleEntity extends ThrowableItemProjectile {
    private boolean impacted;
    public TaintBottleEntity(EntityType<? extends TaintBottleEntity> type,Level level) { super(type,level); }
    public TaintBottleEntity(Level level,LivingEntity owner) {
        this(TCEntities.TAINT_BOTTLE.get(),level); setOwner(owner);
        float yaw=owner.getYRot()*Mth.DEG_TO_RAD, pitch=owner.getXRot()*Mth.DEG_TO_RAD;
        setPos(owner.getX()-Mth.cos(yaw)*.16,owner.getEyeY()-.1,owner.getZ()-Mth.sin(yaw)*.16);
        setYRot(owner.getYRot()); setXRot(owner.getXRot());
        EldritchProjectileLaunch.shoot(this,random,-Mth.sin(yaw)*Mth.cos(pitch),-Mth.sin(pitch-20*Mth.DEG_TO_RAD),Mth.cos(yaw)*Mth.cos(pitch),.5f,1);
    }
    void seedImpact(long seed) { random.setSeed(seed); }
    @Override protected Item getDefaultItem() { return TCItems.TAINT_BOTTLE.get(); }
    @Override protected float getGravity() { return .05f; }
    @Override protected boolean canHitEntity(Entity entity) { return entity.isPickable() && !entity.isSpectator() && (entity!=getOwner() || tickCount>=5); }
    @Override protected void onHit(HitResult hit) {
        if (impacted || isRemoved()) return;
        impacted=true;
        if (!(level() instanceof ServerLevel level)) return;
        for (var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(5))) {
            if (!(living instanceof TaintedMob) && !living.isInvertedHealAndHarm()) living.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),100,0,false,true));
        }
        int x=blockPosition().getX(), y=blockPosition().getY(), z=blockPosition().getZ();
        for(int attempt=0;attempt<10;attempt++) {
            var pos=new BlockPos(x+(int)((random.nextFloat()-random.nextFloat())*5),y,z+(int)((random.nextFloat()-random.nextFloat())*5));
            if (!level.random.nextBoolean() || !level.hasChunkAt(pos) || level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(TCBiomes.TAINTED_LAND)) continue;
            if (!WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND)) continue;
            var floor=level.getBlockState(pos.below());
            if (floor.isRedstoneConductor(level,pos.below()) && level.getBlockState(pos).canBeReplaced()) {
                var fibre=TCArcane.TAINT_FIBRES.get(); level.setBlock(pos,TaintFibreBlock.faces(fibre.defaultBlockState(),level,pos),3);
            }
        }
        level.broadcastEntityEvent(this,(byte)3); discard();
    }
    @Override public void handleEntityEvent(byte event) {
        if (event==3) tc4revival.client.TaintBottleEffects.burst(this); else super.handleEntityEvent(event);
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
}
