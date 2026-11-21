package heartwork.compat.blade;

import java.util.UUID;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** r88 quick-drive: a three-block cube, repeated hits, and 1.05 acceleration. */
public class HeartworkDriveEntity extends EntityDrive {
    private UUID ownerUuid;
    public HeartworkDriveEntity(EntityType<? extends mods.flammpfeil.slashblade.entity.Projectile> type,Level level) {super(type,level);}
    @Override public void setOwner(Entity owner) {super.setOwner(owner);if(owner!=null)ownerUuid=owner.getUUID();}
    @Override public Entity getOwner() {
        if(level() instanceof ServerLevel server && ownerUuid!=null) {
            Entity owner=server.getEntity(ownerUuid);if(owner!=null)super.setOwner(owner);return owner;
        }return super.getOwner();
    }
    @Override public void tick() {
        baseTick();
        if(level().isClientSide)return;
        if(getOwner() instanceof LivingEntity owner) {
            var box=new AABB(position().subtract(1.5,1.5,1.5),position().add(1.5,1.5,1.5));
            for(var projectile:level().getEntitiesOfClass(Projectile.class,box,p -> p!=this&&p.getOwner()!=owner))HeartworkArts.deflect(owner,projectile,(float)getDamage());
            if(tickCount%2==0)for(var victim:level().getEntitiesOfClass(LivingEntity.class,box,e -> HeartworkArts.validTarget(owner,e))) {
                victim.invulnerableTime=0;
                victim.hurt(new net.minecraft.world.damagesource.DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("heartwork:phantom"))),this,owner),(float)Math.max(1,getDamage()));
                var blade=owner.getMainHandItem();if(blade.getItem() instanceof ItemSlashBlade item)item.hurtEnemy(blade,victim,owner);
            }
        }
        if(!level().getBlockState(blockPosition()).getCollisionShape(level(),blockPosition()).isEmpty()) {discard();return;}
        setDeltaMovement(getDeltaMovement().scale(1.05));setPos(position().add(getDeltaMovement()));
        if(tickCount>=getLifetime())discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);if(ownerUuid!=null)tag.putUUID("HeartworkOwner",ownerUuid);tag.putInt("HeartworkAge",tickCount);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);ownerUuid=tag.hasUUID("HeartworkOwner")?tag.getUUID("HeartworkOwner"):null;tickCount=tag.getInt("HeartworkAge");}
}
