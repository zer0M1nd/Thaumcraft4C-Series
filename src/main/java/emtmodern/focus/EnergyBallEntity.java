package emtmodern.focus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
public class EnergyBallEntity extends Projectile implements IEntityAdditionalSpawnData {
    protected static final EntityDataAccessor<Boolean> GROUNDED=SynchedEntityData.defineId(EnergyBallEntity.class,EntityDataSerializers.BOOLEAN);
    public Vec3 acceleration=Vec3.ZERO;
    public int groundedTicks=1;
    public EnergyBallEntity(EntityType<? extends EnergyBallEntity> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(GROUNDED,false);}
    public boolean grounded(){return entityData.get(GROUNDED);}
    public void ground(){entityData.set(GROUNDED,true);}
    public void fire(LivingEntity owner){setOwner(owner);setPos(owner.getX(),owner.getEyeY(),owner.getZ());Vec3 aim=owner.getLookAngle().scale(100).add(random.nextGaussian()*.4,random.nextGaussian()*.4,random.nextGaussian()*.4);acceleration=aim.normalize().scale(.1);setDeltaMovement(Vec3.ZERO);setYRot(owner.getYRot());setXRot(owner.getXRot());}
    @Override public boolean isPickable(){return true;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return true;}
    @Override public void tick(){
        super.tick();
        if(grounded()){
            if(++groundedTicks>=60)discard();
            // A bounded entity query replaces the old 4x4 full-chunk scan without loading chunks.
            // Preserve the original magic damage type while retaining the projectile
            // and shooter for attribution and allied-caster protection events.
            var source=new net.minecraft.world.damagesource.DamageSource(damageSources().magic().typeHolder(),this,getOwner());
            for(var entity:level().getEntities(this,getBoundingBox().inflate(8))){double distance=entity.position().distanceTo(position());if(distance>8)continue;if(!level().isClientSide)entity.hurt(source,(float)(8-distance));else emtmodern.client.EnergyBallEffects.target(this,entity);}
        }else{
            Vec3 motion=getDeltaMovement(),next=position().add(motion);if(level().clip(new ClipContext(position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS)ground();setPos(next);
            float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)+90,pitch=(float)(Math.atan2(motion.horizontalDistance(),motion.y)*180/Math.PI)-90;
            setYRot(yRotO+net.minecraft.util.Mth.wrapDegrees(yaw-yRotO)*.2f);setXRot(xRotO+net.minecraft.util.Mth.wrapDegrees(pitch-xRotO)*.2f);setDeltaMovement(motion.add(acceleration).scale(.85));
        }
        if(level().isClientSide)emtmodern.client.EnergyBallEffects.arcs(this);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("inTile",grounded());tag.putInt("groundedTicks",groundedTicks);tag.putDouble("ax",acceleration.x);tag.putDouble("ay",acceleration.y);tag.putDouble("az",acceleration.z);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(GROUNDED,tag.getBoolean("inTile"));groundedTicks=Math.max(1,Math.min(60,tag.getInt("groundedTicks")));acceleration=new Vec3(tag.getDouble("ax"),tag.getDouble("ay"),tag.getDouble("az"));if(!Double.isFinite(acceleration.lengthSqr())||acceleration.lengthSqr()>1)acceleration=Vec3.ZERO;}
    @Override public void writeSpawnData(FriendlyByteBuf out){out.writeDouble(acceleration.x);out.writeDouble(acceleration.y);out.writeDouble(acceleration.z);}
    @Override public void readSpawnData(FriendlyByteBuf in){acceleration=new Vec3(in.readDouble(),in.readDouble(),in.readDouble());}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
