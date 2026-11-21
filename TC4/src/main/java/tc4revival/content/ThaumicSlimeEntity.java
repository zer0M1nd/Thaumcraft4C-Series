package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public  class ThaumicSlimeEntity extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(ThaumicSlimeEntity.class, EntityDataSerializers.INT);
    private int jumpDelay, launched = 10, spitDelay = 100;
    private boolean split;
    public float squish, previousSquish;
    private float squishTarget;
    public ThaumicSlimeEntity(EntityType<? extends ThaumicSlimeEntity> type, Level level) {
        super(type, level);
        moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this) { @Override public void tick() {} };
        int initial = 1 << random.nextInt(3); jumpDelay = random.nextInt(20) + 10; setSlimeSize(initial);
    }
    public static AttributeSupplier.Builder attributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.MOVEMENT_SPEED, .1).add(Attributes.ATTACK_DAMAGE, 1); }
    @Override protected void registerGoals() {}
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SIZE, 1); }
    public int slimeSize() { return entityData.get(SIZE); }
    public void setSlimeSize(int size) { setSlimeSize(size,true); }
    private void setSlimeSize(int size,boolean heal) {
        float health=getHealth();
        size = Mth.clamp(size, 1, 100);
        entityData.set(SIZE, size); refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(size);
        setHealth(heal?getMaxHealth():Math.min(health,getMaxHealth())); xpReward = (int)Math.sqrt(size);
    }
    @Override public EntityDimensions getDimensions(Pose pose) {
        float root = (float)Math.sqrt(slimeSize());
        float width = .6f*root;
        return EntityDimensions.scalable(width, width);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if (SIZE.equals(key)) refreshDimensions(); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Size", slimeSize()-1); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSlimeSize((int)Math.max(1, Math.min(100, (long)tag.getInt("Size")+1)),false);
        if(tag.contains("Health"))setHealth(Mth.clamp(tag.getFloat("Health"),0,getMaxHealth()));
    }
    @Override public void tick() {
        if (!level().isClientSide && level().getDifficulty() == Difficulty.PEACEFUL) { discard(); return; }
        squish += (squishTarget-squish)*.5f; previousSquish = squish;
        boolean grounded = onGround(); super.tick();
        int root = (int)Math.sqrt(slimeSize());
        if (launched > 0) {
            launched--;
            if (level().isClientSide) tc4revival.client.ThaumicSlimeParticles.spawn(this, root, root*(launched+1));
        }
        if (onGround() && !grounded) {
            if (level().isClientSide) tc4revival.client.ThaumicSlimeParticles.spawn(this, root, root*8);
            if (slimeSize()>5) playSound(slimeSound(), getSoundVolume(), (1+(random.nextFloat()-random.nextFloat())*.2f)/.8f);
            squishTarget = -.5f;
        } else if (!onGround() && grounded) squishTarget = 1;
        squishTarget *= .6f;
    }
    @Override protected void customServerAiStep() {
        setSpeed(.1f);
        Player player = level().getNearestPlayer(this, 16);
        if (player != null && (!player.isAlive() || player.isCreative() || player.isSpectator())) player = null;
        // Filter before nearest selection so a closer creative player cannot mask a vulnerable player.
        if (player == null) player = level().getNearestPlayer(getX(), getY(), getZ(), 16,
                candidate -> candidate instanceof Player target && target.isAlive() && !target.isCreative() && !target.isSpectator());
        if (player != null) {
            if (spitDelay > 0) spitDelay--;
            face(player);
            if (distanceTo(player)>4 && spitDelay<=0 && slimeSize()>3) {
                spitDelay=101;
                var child = TCEntities.THAUMIC_SLIME.get().create(level());
                if (child != null) { child.setSlimeSize(1); child.launchFrom(this, player); level().addFreshEntity(child); }
                playSound(TCSounds.GORE.get(), 1, .8f*(1+(random.nextFloat()-random.nextFloat())*.2f));
                setSlimeSize(slimeSize()-1,false);
            }
        } else {
            ThaumicSlimeEntity closest = null; double distance = Double.MAX_VALUE;
            for (var slime : level().getEntitiesOfClass(ThaumicSlimeEntity.class, new AABB(position(), position()).inflate(16,8,16))) {
                double next = distanceToSqr(slime);
                if (slime != this && slime.isAlive() && slime.tickCount>100 && slime.slimeSize()+slimeSize()<=100 && next<distance) {closest=slime;distance=next;}
            }
            if (closest != null) {
                face(closest);
                if (distanceTo(closest)<getBbWidth()+closest.getBbWidth()) {
                    float health=closest.getHealth()+getHealth();closest.setSlimeSize(closest.slimeSize()+slimeSize(),false);closest.setHealth(Math.min(health,closest.getMaxHealth()));discard();return;
                }
            }
        }
        if (onGround() && jumpDelay--<=0) {
            jumpDelay=random.nextInt(16)+8; if (player!=null) jumpDelay/=3;
            setJumping(true);
            if (slimeSize()>3) playSound(slimeSound(), getSoundVolume(), .8f*(1+(random.nextFloat()-random.nextFloat())*.2f));
            xxa=1-random.nextFloat()*2; zza=(float)Math.sqrt(slimeSize());
        } else { setJumping(false); if (onGround()) { xxa=0; zza=0; } }
    }
    private void face(LivingEntity target) {
        var delta = target.getEyePosition().subtract(getEyePosition());
        setYRot(Mth.approachDegrees(getYRot(), (float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90, 10));
        setXRot(Mth.approachDegrees(getXRot(), (float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI), 20));
        setYHeadRot(getYRot());
    }
    private void launchFrom(LivingEntity parent, LivingEntity target) {
        double middle = (parent.getBoundingBox().minY+parent.getBoundingBox().maxY)/2;
        var horizontal = target.position().subtract(parent.position()).multiply(1,0,1);
        double length = horizontal.length();
        moveTo(parent.getX(), middle, parent.getZ(), parent.getYRot(), 0);
        if (length<1e-7) return;
        var origin = parent.position().add(horizontal.scale(1/length)); setPos(origin.x,middle,origin.z);
        var direction = new Vec3(horizontal.x, target.getBoundingBox().minY+target.getBbHeight()/3-middle+length*.2, horizontal.z);
        if (direction.lengthSqr()<1e-14) return;
        direction=direction.scale(1/(float)direction.length()).add(random.nextGaussian()*.0075,random.nextGaussian()*.0075,random.nextGaussian()*.0075).scale(1.5);
        setDeltaMovement(direction);
        setYRot((float)(Math.atan2(direction.x,direction.z)*180/Math.PI)); yRotO=getYRot();
        setXRot((float)(Math.atan2(direction.y,direction.horizontalDistance())*180/Math.PI)); xRotO=getXRot();
    }
    @Override public void playerTouch(Player player) {
        if (level().isClientSide || !isAlive()) return;
        int radius=(int)Math.max(1,Math.sqrt(slimeSize())); if (launched>0 && radius==2) radius=3;
        if (hasLineOfSight(player) && distanceToSqr(player)<.64*radius*radius && player.hurt(damageSources().mobAttack(this),slimeSize()))
            playSound(SoundEvents.PLAYER_ATTACK_STRONG,1,1+(random.nextFloat()-random.nextFloat())*.2f);
    }
    @Override public void remove(RemovalReason reason) {
        if (!split && reason==RemovalReason.KILLED && !level().isClientSide && getHealth()<=0) {
            split=true; int root=(int)Math.sqrt(slimeSize());
            if (root>1) for (int i=0;i<root;i++) {
                var child=TCEntities.THAUMIC_SLIME.get().create(level()); if (child==null) continue;
                child.setSlimeSize(1); child.moveTo(getX()+((i%2)-.5)*root/4,getY()+.5,getZ()+((i/2)-.5)*root/4,random.nextFloat()*360,0);
                level().addFreshEntity(child);
            }
        }
        super.remove(reason);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) { }
    private SoundEvent slimeSound() { return slimeSize()>3 ? SoundEvents.SLIME_SQUISH : SoundEvents.SLIME_SQUISH_SMALL; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return slimeSound(); }
    @Override protected SoundEvent getDeathSound() { return slimeSound(); }
    @Override protected float getSoundVolume() { return .1f*(float)Math.sqrt(slimeSize()); }
    @Override public int getMaxHeadXRot() { return 0; }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
