package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Rooted taint predator. Target selection deliberately includes non-tainted animals and villagers. */
public class TaintacleEntity extends Monster implements TaintedMob {
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumcraft:tentacle"));
    protected int attackCooldown;
    private float flail=1;
    public TaintacleEntity(EntityType<? extends TaintacleEntity> type,Level level) { super(type,level); xpReward=10; }
    public static AttributeSupplier.Builder attributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,7); }
    @Override protected void registerGoals() { }
    @Override public void move(MoverType type,Vec3 movement) { super.move(type,new Vec3(0,Math.min(0,movement.y),0)); }
    @Override public boolean isPushable() { return true; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean isPickable() { return true; }
    public float flailIntensity() { return flail; }
    public int attackCooldown() { return attackCooldown; }
    public boolean agitated() { return getTarget()!=null && distanceToSqr(getTarget())<getBbHeight()*7*(getBbHeight()*7); }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) {
        var state=level.getBlockState(blockPosition());
        // Natural spawn positions are the entity's feet, above a solid soil block.
        // Fibres occupy that foot position themselves because they have no collision.
        boolean substrate=state.getBlock() instanceof TaintFibreBlock && state.getValue(TaintFibreBlock.VARIANT)==0
                || level.getBlockState(blockPosition().below()).is(TCArcane.TAINT_SOIL.get());
        return substrate && TCBiomes.isTainted(level(),blockPosition())
                && level().getEntitiesOfClass(TaintacleEntity.class,new AABB(position(),position()).inflate(24,8,24)).isEmpty()
                && super.checkSpawnRules(level,reason);
    }
    @Override public void tick() {
        super.tick(); if (attackCooldown>0) attackCooldown--;
        if (!level().isClientSide && tickCount%20==0 && !TCBiomes.isTainted(level(),blockPosition())) actuallyHurt(damageSources().starve(),1);
        if (level().isClientSide) {
            boolean thrash=tickCount>getBbHeight()*10 && (hurtTime>0 || attackCooldown>0 || getTarget()!=null && distanceTo(getTarget())<getBbHeight());
            if (thrash && flail<3) flail+=.2f; else if (!thrash && flail>1) flail-=.2f;
            if (tickCount<getBbHeight()*10 && onGround()) tc4revival.client.ThaumicSlimeParticles.tentacleArise(this);
        }
        if (getTarget()==null) {
            LivingEntity nearest=null; double distance=Double.MAX_VALUE;
            for (var entity:level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(getBbHeight()*6,getBbHeight()*3,getBbHeight()*6))) {
                double candidate=distanceToSqr(entity);
                if (!(entity instanceof TaintedMob) && candidate<distance) { nearest=entity; distance=candidate; }
            }
            setTarget(nearest);
        } else if (!getTarget().isAlive() || !agitated()) setTarget(null);
        else {
            var target=getTarget(); float yaw=(float)Math.toDegrees(Math.atan2(target.getZ()-getZ(),target.getX()-getX()))-90;
            setYRot(getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-getYRot()),-5,5)); yHeadRot=getYRot(); yBodyRot=getYRot();
            if (!level().isClientSide && hasLineOfSight(target) && attackCooldown<=0) {
                if (distanceTo(target)<=getBbHeight() && target.getBoundingBox().maxY>getBoundingBox().minY && target.getBoundingBox().minY<getBoundingBox().maxY) {
                    attackCooldown=20; level().broadcastEntityEvent(this,(byte)4); doHurtTarget(target); playSound(TCSounds.TENTACLE.get(),getSoundVolume(),getVoicePitch());
                } else if (distanceTo(target)>getBbHeight() && target.onGround() && !(this instanceof SmallTaintacleEntity)) spawnTentacle(target);
            }
        }
    }
    @Override public boolean doHurtTarget(Entity target) {
        float damage=(float)getAttributeValue(Attributes.ATTACK_DAMAGE);
        // Current potion attribute modifiers differ from TC4's integer shift rules.
        if (hasEffect(MobEffects.DAMAGE_BOOST)) { int amp=getEffect(MobEffects.DAMAGE_BOOST).getAmplifier(); damage-=3*(amp+1); damage+=(3<<amp); }
        if (hasEffect(MobEffects.WEAKNESS)) { int amp=getEffect(MobEffects.WEAKNESS).getAmplifier(); damage+=4*(amp+1); damage-=(2<<amp); }
        int knockback=EnchantmentHelper.getKnockbackBonus(this);
        if (target instanceof LivingEntity living) damage+=EnchantmentHelper.getDamageBonus(getMainHandItem(),living.getMobType());
        boolean hit=target.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),this),damage);
        if (hit) {
            if (knockback>0) { target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*knockback*.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*knockback*.5); setDeltaMovement(getDeltaMovement().multiply(.6,1,.6)); }
            int fire=EnchantmentHelper.getFireAspect(this); if (fire>0) target.setSecondsOnFire(fire*4);
            if (target instanceof LivingEntity living) EnchantmentHelper.doPostHurtEffects(living,this);
            EnchantmentHelper.doPostDamageEffects(this,target);
        }
        return hit;
    }
    protected void spawnTentacle(Entity target) {
        if (!(level() instanceof ServerLevel server)) return;
        var pos=BlockPos.containing(target.getX(),target.getBoundingBox().minY,target.getZ());
        boolean outer=server.getBiome(pos).is(TCBiomes.OUTER_LANDS),tainted=TCBiomes.isTainted(server,pos);
        if (!outer && !(tainted && (taintMaterial(server.getBlockState(pos)) || taintMaterial(server.getBlockState(pos.below()))))) return;
        attackCooldown=40+server.random.nextInt(20); var child=TCEntities.TAINTACLE_SMALL.get().create(server);
        if (child!=null) { child.moveTo(target.getX()+server.random.nextFloat()-server.random.nextFloat(),target.getY(),target.getZ()+server.random.nextFloat()-server.random.nextFloat(),0,0); server.addFreshEntity(child); }
        playSound(TCSounds.TENTACLE.get(),getSoundVolume(),getVoicePitch());
        if (outer && server.isEmptyBlock(pos)) {
            boolean adjacent=false; for (Direction side:Direction.values()) if (server.getBlockState(pos.relative(side)).isSolidRender(server,pos.relative(side))) { adjacent=true; break; }
            if (adjacent) {
                WorldBiomeChanges.changeColumn(server,pos,TCBiomes.TAINTED_LAND);
                var fibres=TCArcane.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibreBlock.VARIANT,server.random.nextInt(4)==0 ? 1 : 0);
                server.setBlock(pos,TaintFibreBlock.faces(fibres,server,pos),3);
            }
        }
    }
    private static boolean taintMaterial(BlockState state) { return state.getBlock() instanceof TaintFibreBlock || state.getBlock() instanceof TaintSolidBlock; }
    @Override public boolean hurt(DamageSource source,float amount) {
        if (!level().isClientSide && !(this instanceof SmallTaintacleEntity) && source.getEntity()!=null && distanceTo(source.getEntity())>16) spawnTentacle(source.getEntity());
        return super.hurt(source,amount);
    }
    @Override public void handleEntityEvent(byte event) { if (event==4) attackCooldown=20; else super.handleEntityEvent(event); }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override public float getVoicePitch() { return 1.3f-getBbHeight()/10; }
    @Override protected float getSoundVolume() { return getBbHeight()/8; }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.ROOTS.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TCSounds.TENTACLE.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.TENTACLE.get(); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
}
