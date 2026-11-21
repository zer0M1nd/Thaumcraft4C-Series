package tc4revival.content;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;

/** Stationary colony with a persistent reproduction countdown. */
public  class TaintHiveEntity extends TaintSporeEntity {
    private int spawnCounter=500;
    public TaintHiveEntity(EntityType<? extends TaintHiveEntity> type,Level level) { super(type,level);setSporeSize(10); }
    public static AttributeSupplier.Builder attributes() { return TaintSporeEntity.attributes().add(Attributes.MAX_HEALTH,75); }
    @Override public EntityDimensions getDimensions(Pose pose) { return EntityDimensions.scalable(1,1); }
    public int spawnCounter() { return spawnCounter; }
    @Override protected void sporeActivity() {
        moveTowardsClosestSpace(getX(),getY(),getZ());
        if(spawnCounter>0)spawnCounter--;
        if(spawnCounter<=0 && level().getNearestPlayer(getX(),getY(),getZ(),16,true)!=null) { spawnCounter=500;releaseSwarm(); }
        if(level().isClientSide)tc4revival.client.TaintSporeParticles.maintainHive(this);
        if(deathTime==1)releaseSwarm();
    }
    @Override public void playerTouch(Player player) {}
    @Override public void burst() { releaseSwarm(); }
    private void releaseSwarm() {
        if(level().isClientSide)return;
        playSound(TCSounds.GORE.get(),1,.9f+level().random.nextFloat()*.1f);
        var child=TCEntities.TAINT_SWARM.get().create(level());
        if(child!=null) { child.moveTo(getX(),getY()+.5,getZ(),level().random.nextFloat()*360,0);level().addFreshEntity(child); }
        level().broadcastEntityEvent(this,(byte)6);
    }
    @Override public void handleDamageEvent(DamageSource source) {
        if(level().isClientSide)tc4revival.client.ThaumicSlimeParticles.sploosh(this,10);
        super.handleDamageEvent(source);
    }
    @Override public net.minecraft.world.phys.Vec3 getLightProbePosition(float partial) { return position().add(0,getBbHeight()*.66,0); }
    @Override public void handleEntityEvent(byte event) {
        if(event==6) { spawnCounter=500;tc4revival.client.ThaumicSlimeParticles.sploosh(this,25);tc4revival.client.TaintSporeParticles.clear(this); }
        else super.handleEntityEvent(event);
    }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.ROOTS.get(); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("SpawnCounter",spawnCounter);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);spawnCounter=tag.contains("SpawnCounter")?Math.max(0,Math.min(500,tag.getInt("SpawnCounter"))):500;}
}
