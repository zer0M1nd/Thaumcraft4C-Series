package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import tc4revival.content.TCEffects;
import tc4revival.aspect.TCAspects;
import thaumcraft.api.fx.LegacyEffects;

/** Ballistic wisp with persisted ownership, lifetime and original area discharge. */
public final class ChargedWisp extends Projectile {
    private static final EntityDataAccessor<String> ASPECT=SynchedEntityData.defineId(ChargedWisp.class,EntityDataSerializers.STRING);
    private int age,lifetime=40,power;
    public ChargedWisp(EntityType<? extends ChargedWisp> type,Level level) { super(type,level); }
    @Override protected void defineSynchedData() { entityData.define(ASPECT,"aer"); }
    public String aspectTag() { return entityData.get(ASPECT); }
    public void initialize(Entity owner,String aspect,int power,int extend) {
        setOwner(owner);entityData.set(ASPECT,aspect);this.power=Math.max(0,power);
        lifetime=20+random.nextInt(20)+20*Math.max(0,extend);
        setDeltaMovement(owner.getLookAngle());
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && (!level().hasChunkAt(blockPosition()) || age++>lifetime)){discard();return;}
        move(MoverType.SELF,getDeltaMovement());
        if(level().isClientSide)return;
        int color=TCAspects.catalog().find(aspectTag()).map(a->a.color()).orElse(0xffffff);
        var owner=getOwner();
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(4))) {
            if(target==owner || (owner!=null && owner.isAlliedTo(target)))continue;
            if(target instanceof BrightestOne boss && boss.shortCircuit(this)){discard();return;}
            target.hurt(damageSources().indirectMagic(this,owner),10+power);
            var exhaustion=new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),(int)Math.min(Integer.MAX_VALUE,1000L*power),30);
            exhaustion.setCurativeItems(java.util.List.of());target.addEffect(exhaustion);
            ConciliumArcNetwork.emit(level(),target.position().add(random.nextFloat(),random.nextFloat(),random.nextFloat()),position().add(0,getEyeHeight()/2,0),color,.1f);
            if(random.nextInt(50)==0)playSound(tc4revival.content.TCSounds.JACOBS.get(),.5f,1+(random.nextFloat()-random.nextFloat())*.2f);
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("Life",age);tag.putInt("LifeTime",lifetime);tag.putInt("power",power);tag.putString("type",aspectTag());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);age=Math.max(0,tag.getInt("Life"));lifetime=Math.max(1,tag.getInt("LifeTime"));power=Math.max(0,tag.getInt("power"));
        entityData.set(ASPECT,tag.getString("type"));
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
