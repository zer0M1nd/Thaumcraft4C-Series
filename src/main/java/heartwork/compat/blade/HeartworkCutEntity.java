package heartwork.compat.blade;

import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** FIX EntitySA interrupts and displaces targets; its constructed damage source
 * was never applied. Do not replace this with the host's damaging judgement cut. */
public class HeartworkCutEntity extends EntityJudgementCut {
    private java.util.UUID ownerUuid;
    public HeartworkCutEntity(EntityType<? extends mods.flammpfeil.slashblade.entity.Projectile> type,Level level) {super(type,level);}
    @Override public void setOwner(Entity owner) {super.setOwner(owner);if(owner!=null)ownerUuid=owner.getUUID();}
    @Override public Entity getOwner() {
        if(level() instanceof net.minecraft.server.level.ServerLevel server && ownerUuid!=null) {
            Entity owner=server.getEntity(ownerUuid);if(owner!=null)super.setOwner(owner);return owner;
        }return super.getOwner();
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {super.addAdditionalSaveData(tag);if(ownerUuid!=null)tag.putUUID("HeartworkOwner",ownerUuid);tag.putInt("HeartworkAge",tickCount);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {super.readAdditionalSaveData(tag);ownerUuid=tag.hasUUID("HeartworkOwner")?tag.getUUID("HeartworkOwner"):null;tickCount=tag.getInt("HeartworkAge");}
    @Override public void tick() {
        baseTick();
        if(!level().isClientSide && getShooter() instanceof LivingEntity owner) {
            if(tickCount<8 && tickCount%2==0)playSound(net.minecraft.sounds.SoundEvents.WITHER_HURT,0.2F,0.5F+random.nextFloat()*0.25F);
            for(Projectile projectile:level().getEntitiesOfClass(Projectile.class,getBoundingBox(),p -> p!=this && p.getOwner()!=owner))HeartworkArts.deflect(owner,projectile,1);
            if(tickCount%2==0)for(LivingEntity victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox(),e -> HeartworkArts.validTarget(owner,e))) {
                if(victim instanceof net.minecraft.world.entity.monster.EnderMan)victim.getPersistentData().putLong("HeartworkTeleportBlockedUntil",level().getGameTime()+200);
                victim.invulnerableTime=0;
                var stack=owner.getMainHandItem();
                if(stack.getItem() instanceof ItemSlashBlade blade)blade.hurtEnemy(stack,victim,owner);
                victim.setDeltaMovement(Vec3.ZERO);
                if(tickCount>3) {
                    int fortune=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,stack);
                    double force=fortune>0?-fortune*0.5:0.5;
                    victim.push(-Math.sin(Math.toRadians(owner.getYRot()))*force,0.2,Math.cos(Math.toRadians(owner.getYRot()))*force);
                }
                victim.hurtMarked=true;
            }
        }
        if(tickCount>=getLifetime())discard();
    }
}
