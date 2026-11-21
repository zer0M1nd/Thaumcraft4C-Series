package heartwork.compat.blade;

import java.util.UUID;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Legacy blistering swords, with separate waiting, flight and attached clocks. */
public class HeartworkSummonedSword extends EntityAbstractSummonedSword {
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(HeartworkSummonedSword.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(HeartworkSummonedSword.class,EntityDataSerializers.INT);
    private static final int[][] PATTERN={{1,1,0},{-1,1,0},{1,0,1},{-1,0,1},{1,-1,2},{-1,-1,2},{1,-2,3},{-1,-2,3}};
    private UUID ownerUuid,targetUuid;
    private int age;
    private Vec3 attachment=Vec3.ZERO;
    private ItemStack blade=ItemStack.EMPTY;
    public HeartworkSummonedSword(EntityType<? extends mods.flammpfeil.slashblade.entity.Projectile> type,Level level) {super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(TARGET,-1);entityData.define(PHASE,0);}
    @Override public void setOwner(Entity owner) {
        super.setOwner(owner);
        if(owner!=null) {ownerUuid=owner.getUUID();if(owner instanceof LivingEntity living)blade=living.getMainHandItem();}
    }
    @Override public Entity getOwner() {
        if(level() instanceof ServerLevel server && ownerUuid!=null) {
            Entity owner=server.getEntity(ownerUuid);if(owner!=null)super.setOwner(owner);return owner;
        }
        return super.getOwner();
    }
    public Entity target() {
        if(level() instanceof ServerLevel server && targetUuid!=null) {
            Entity target=server.getEntity(targetUuid);if(target!=null)entityData.set(TARGET,target.getId());return target;
        }
        return level().getEntity(entityData.get(TARGET));
    }
    public int phaseAge() {return age;}
    public boolean hasFired() {return entityData.get(PHASE)>0;}
    public void configure(Entity target,int delay,int slot) {targetUuid=target.getUUID();entityData.set(TARGET,target.getId());setDelay(delay);orbit(slot);}
    private void orbit(int index) {
        Entity owner=getOwner();if(owner==null)return;
        int[] pattern=PATTERN[Mth.clamp(index,0,7)];
        double yaw=Math.toRadians(-owner.getYRot()+90*pattern[0]),distance=0.8+0.15*pattern[2];
        Vec3 offset=new Vec3(Math.sin(yaw)*distance,owner.getEyeHeight()+pattern[1]*0.25,Math.cos(yaw)*distance).subtract(owner.getLookAngle());
        setPos(owner.position().add(offset));setYRot(owner.getYRot());setXRot(owner.getXRot());
    }
    private void aim() {
        Entity target=target();
        if(target!=null && target.isAlive()) {
            Vec3 delta=target.position().add(0,target.getBbHeight()/2,0).subtract(position());
            float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
            float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
            setYRot(Mth.approachDegrees(getYRot(),yaw,10));setXRot(Mth.approachDegrees(getXRot(),pitch,10));
            // Targeting in r88 resets the accelerated vector to 1.75 every tick.
            setDeltaMovement(getLookAngle().scale(1.75));
        } else setDeltaMovement(getDeltaMovement().scale(isInWater()?1:1.1));
    }
    @Override public Entity getHitEntity() {return entityData.get(PHASE)==2?target():null;}
    @Override public void setHitEntity(Entity target) {
        targetUuid=target.getUUID();entityData.set(TARGET,target.getId());entityData.set(PHASE,2);age=0;
        attachment=position().subtract(target.position()).yRot((float)Math.toRadians(target.getYRot()));setDeltaMovement(Vec3.ZERO);
    }
    @Override public void tick() {
        baseTick();
        if(level().isClientSide) {
            if(hasFired())level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,getX(),getY(),getZ(),0,0,0);
            return;
        }
        Entity owner=getOwner();if(owner==null || !owner.isAlive()) {burst();return;}
        if(entityData.get(PHASE)==0) {
            // FIX overwrites the constructor's formation slot with its launch interval.
            orbit(getDelay());aim();
            if(level().getGameTime()>owner.getPersistentData().getLong("SB.BSHOLDLIMIT")+getDelay()) {
                entityData.set(PHASE,1);age=0;playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP,0.35F,0.2F);
            }
            return;
        }
        age++;
        if(entityData.get(PHASE)==2) {
            Entity target=target();if(target==null || !target.isAlive()) {burst();return;}
            setPos(target.position().add(attachment.yRot((float)Math.toRadians(-target.getYRot()))));
            if(age>=200) {damage(target,(float)Math.max(1,getDamage()/2),false);burst();}return;
        }
        if(age>=30) {burst();return;}
        aim();Vec3 start=position(),end=start.add(getDeltaMovement());
        for(Projectile projectile:level().getEntitiesOfClass(Projectile.class,getBoundingBox().expandTowards(getDeltaMovement()).inflate(1),p -> p!=this&&p.getOwner()!=owner)) {
            if(projectile.getBoundingBox().inflate(0.3).clip(start,end).isPresent()) {HeartworkArts.deflect(owner,projectile,(float)getDamage());burst();return;}
        }
        Entity target=target();
        if(owner instanceof LivingEntity living && HeartworkArts.validTarget(living,target) &&
                (target.getBoundingBox().inflate(0.3).contains(start)||target.getBoundingBox().inflate(0.3).clip(start,end).isPresent())) {
            setHitEntity(target);damage(target,(float)Math.max(1,getDamage()),true);return;
        }
        if(age>=10 && level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS) {burst();return;}
        if(age>getDelay())setPos(end);
    }
    private void damage(Entity target,float amount,boolean stun) {
        Entity owner=getOwner();if(!(owner instanceof LivingEntity living)||!HeartworkArts.validTarget(living,target))return;
        target.invulnerableTime=0;
        target.hurt(new DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("heartwork:phantom"))),this,owner),amount);
        if(target instanceof LivingEntity victim) {
            if(blade.getItem() instanceof ItemSlashBlade item)item.hurtEnemy(blade,victim,living);
            victim.setDeltaMovement(0,0.1,0);victim.hurtTime=1;victim.hurtMarked=true;if(stun)StunManager.setStun(victim);
        }
    }
    @Override public void burst() {
        if(isRemoved())return;
        if(!level().isClientSide && getOwner() instanceof LivingEntity living) {
            for(LivingEntity victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(1),e -> HeartworkArts.validTarget(living,e)&&e!=getHitEntity()))damage(victim,1,true);
            ((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,getX(),getY(),getZ(),16,0.5,0.5,0.5,0.25);
        }
        playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK,0.25F,1.6F);discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(ownerUuid!=null)tag.putUUID("HeartworkOwner",ownerUuid);if(targetUuid!=null)tag.putUUID("HeartworkTarget",targetUuid);
        tag.putInt("HeartworkPhase",entityData.get(PHASE));tag.putInt("HeartworkAge",age);tag.putInt("HeartworkDelay",getDelay());
        tag.putDouble("HeartworkDamage",getDamage());tag.putInt("HeartworkColor",getColor());
        tag.putDouble("HitX",attachment.x);tag.putDouble("HitY",attachment.y);tag.putDouble("HitZ",attachment.z);tag.put("HeartworkBlade",blade.save(new CompoundTag()));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerUuid=tag.hasUUID("HeartworkOwner")?tag.getUUID("HeartworkOwner"):null;targetUuid=tag.hasUUID("HeartworkTarget")?tag.getUUID("HeartworkTarget"):null;
        entityData.set(PHASE,tag.getInt("HeartworkPhase"));age=tag.getInt("HeartworkAge");setDelay(tag.getInt("HeartworkDelay"));
        setDamage(tag.getDouble("HeartworkDamage"));setColor(tag.getInt("HeartworkColor"));
        attachment=new Vec3(tag.getDouble("HitX"),tag.getDouble("HitY"),tag.getDouble("HitZ"));blade=ItemStack.of(tag.getCompound("HeartworkBlade"));
    }
}
