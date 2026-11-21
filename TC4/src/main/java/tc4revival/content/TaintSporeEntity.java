package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.registries.ForgeRegistries;

/** Rooted spore that grows and releases small taint spiders when disturbed. */
public class TaintSporeEntity extends Monster implements TaintedMob,IEntityAdditionalSpawnData {
    public static final IntegerProperty FIBRE_VARIANT=IntegerProperty.create("variant",0,4);
    private static final ResourceLocation FIBRES=ResourceLocation.parse("thaumcraft:taint_fibres");
    private static final EntityDataAccessor<Integer> SIZE=SynchedEntityData.defineId(TaintSporeEntity.class,EntityDataSerializers.INT);
    private int growth;
    private boolean burst;
    public float displaySize;
    public TaintSporeEntity(EntityType<? extends TaintSporeEntity> type,Level level) { super(type,level); setSporeSize(2); }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.ATTACK_DAMAGE,1);
    }
    @Override protected void registerGoals() {}
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SIZE,1); }
    public int sporeSize() { return entityData.get(SIZE); }
    public void setSporeSize(int value) {
        entityData.set(SIZE,Mth.clamp(value,1,10)); refreshDimensions(); xpReward=sporeSize();
    }
    @Override public EntityDimensions getDimensions(Pose pose) {
        float size=Math.max(.15f*sporeSize(),.5f); return EntityDimensions.scalable(size,size);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key); if (SIZE.equals(key)) refreshDimensions();
    }
    private BlockPos support() { return BlockPos.containing(getX(),getBoundingBox().minY-1,getZ()); }
    private boolean anchored() {
        var state=level().getBlockState(support());
        return FIBRES.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))
                && state.hasProperty(FIBRE_VARIANT) && state.getValue(FIBRE_VARIANT)==4;
    }
    @Override public void move(MoverType type,Vec3 motion) {
        if (!anchored()) super.move(type,new Vec3(0,Math.min(0,motion.y),0));
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance<4096; }
    @Override public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (!level().isClientSide && tickCount%20==0 && !LegacyBiomeProfiles.get(level().getBiome(blockPosition())).node().tainted())
            actuallyHurt(damageSources().starve(),1);
        sporeActivity();
    }
    protected void sporeActivity() {
        if (sporeSize()<10 && growth++==1200) { setSporeSize(sporeSize()+1); growth=0; }
        if (level().isClientSide) {
            if (displaySize<sporeSize()) displaySize+=.02f;
            tc4revival.client.TaintSporeParticles.maintain(this);
        }
        if (!anchored() || deathTime>0) burst();
    }
    @Override public void playerTouch(Player player) { burst(); }
    public void burst() {
        if (burst || isRemoved()) return;
        // Only the authoritative side spawns children; the entity event carries the burst effect.
        if (level().isClientSide) return;
        burst=true;
        playSound(TCSounds.GORE.get(),1,.9f+level().random.nextFloat()*.1f);
        int count=sporeSize()/3+level().random.nextInt(sporeSize()/2+1);
        for (int i=0;i<count;i++) {
            var child=new TaintSpiderEntity(TCEntities.TAINT_SPIDER.get(),level());
            child.moveTo(getX()+level().random.nextFloat()-level().random.nextFloat(),getY()+level().random.nextFloat(),
                    getZ()+level().random.nextFloat()-level().random.nextFloat(),level().random.nextFloat()*360,0);
            level().addFreshEntity(child);
        }
        if (anchored()) level().setBlock(support(),level().getBlockState(support()).setValue(FIBRE_VARIANT,3),3);
        level().broadcastEntityEvent(this,(byte)61); discard();
    }
    @Override public void handleEntityEvent(byte event) {
        if (event==61) tc4revival.client.ThaumicSlimeParticles.sploosh(this,50);
        else super.handleEntityEvent(event);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Size",sporeSize()-1); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setSporeSize((int)Math.max(1,Math.min(10,(long)tag.getInt("Size")+1)));
    }
    @Override public void writeSpawnData(FriendlyByteBuf buffer) { buffer.writeFloat(sporeSize()); }
    @Override public void readSpawnData(FriendlyByteBuf buffer) { displaySize=buffer.readFloat(); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override protected float getSoundVolume() { return .1f; }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.SWARM.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TCSounds.GORE.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.GORE.get(); }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
}
