package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public  class TaintedCreeperEntity extends TaintedAnimalEntity {
    private static final EntityDataAccessor<Integer> SWELL=SynchedEntityData.defineId(TaintedCreeperEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> POWERED=SynchedEntityData.defineId(TaintedCreeperEntity.class,EntityDataSerializers.BOOLEAN);
    private int oldFuse,fuse,savedFuse=30,savedRadius=3;
    public TaintedCreeperEntity(EntityType<? extends TaintedCreeperEntity> type,Level level) {
        super(type,level,Kind.CREEPER);
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new SwellGoal());
        goalSelector.addGoal(3,new AvoidEntityGoal<>(this,Ocelot.class,6,1,1.2));
        goalSelector.addGoal(3,new AvoidEntityGoal<>(this,Cat.class,6,1,1.2));
        goalSelector.addGoal(4,new TaintMeleeGoal(this,LivingEntity.class,false));
        goalSelector.addGoal(5,new RandomStrollGoal(this,1));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(2,new HurtByTargetGoal(this));
    }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(SWELL,-1);entityData.define(POWERED,false); }
    public int swell() { return entityData.get(SWELL); }
    public void swell(int direction) { entityData.set(SWELL,direction); }
    public boolean powered() { return entityData.get(POWERED); }
    public float flash(float partial) { return Mth.lerp(partial,oldFuse,fuse)/Math.max(1,savedFuse-2); }
    @Override public boolean doHurtTarget(Entity target) { return true; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) {
        boolean hurt=super.causeFallDamage(distance,multiplier,source);
        fuse=Math.min(fuse+(int)(distance*1.5f),savedFuse-5);return hurt;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(powered())tag.putBoolean("powered",true);
        tag.putInt("Fuse",savedFuse);tag.putInt("ExplosionRadius",savedRadius);tag.putInt("FuseProgress",fuse);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);entityData.set(POWERED,tag.getBoolean("powered"));
        if(tag.contains("Fuse"))savedFuse=Math.max(1,tag.getInt("Fuse"));
        if(tag.contains("ExplosionRadius"))savedRadius=Mth.clamp(tag.getInt("ExplosionRadius"),0,127);
        fuse=Mth.clamp(tag.getInt("FuseProgress"),0,savedFuse);oldFuse=fuse;
    }
    @Override public void tick() {
        if(isAlive()) {
            oldFuse=fuse;
            if(swell()>0 && fuse==0)playSound(SoundEvents.CREEPER_PRIMED,1,.5f);
            fuse=Math.max(0,fuse+swell());
            if(fuse>=savedFuse) { fuse=savedFuse;if(!level().isClientSide)detonate(); }
        }
        super.tick();
    }
    private void detonate() {
        var level=(ServerLevel)level();
        float strength=savedRadius*.5f*(powered()?2:1);
        level.explode(this,getX(),getY()+getBbHeight()/2,getZ(),strength,Level.ExplosionInteraction.NONE);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(6)))
            if(!(target instanceof TaintedMob) && !target.isInvertedHealAndHarm())target.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),100));
        int x=blockPosition().getX(),y=blockPosition().getY(),z=blockPosition().getZ();
        for(int i=0;i<10 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this);i++) {
            var pos=new BlockPos(x+(int)((random.nextFloat()-random.nextFloat())*5),y,z+(int)((random.nextFloat()-random.nextFloat())*5));
            if(!level.random.nextBoolean() || !level.hasChunkAt(pos) || level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(TCBiomes.TAINTED_LAND))continue;
            if(!WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND))continue;
            if(level.getBlockState(pos.below()).isRedstoneConductor(level,pos.below()) && level.getBlockState(pos).canBeReplaced())
                level.setBlock(pos,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,pos),3);
        }
        level.broadcastEntityEvent(this,(byte)61);discard();
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==61)tc4revival.client.TaintedAnimalEffects.explode(this);else super.handleEntityEvent(event);
    }
    private  class SwellGoal extends Goal {
        private LivingEntity victim;
        SwellGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return swell()>0 || getTarget()!=null && distanceToSqr(getTarget())<9; }
        @Override public void start() { getNavigation().stop();victim=getTarget(); }
        @Override public void stop() { victim=null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { swell(victim!=null && distanceToSqr(victim)<=49 && getSensing().hasLineOfSight(victim)?1:-1); }
    }
}
