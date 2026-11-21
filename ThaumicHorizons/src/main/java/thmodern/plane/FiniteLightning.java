package thmodern.plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
/** Vanilla lightning gameplay with a synchronized finite rendering height. */
public final class FiniteLightning extends LightningBolt {
 private static final EntityDataAccessor<Integer> LENGTH=SynchedEntityData.defineId(FiniteLightning.class,EntityDataSerializers.INT);
 public FiniteLightning(EntityType<? extends LightningBolt> type,Level level){super(type,level);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(LENGTH,16);}
 public int length(){return entityData.get(LENGTH);}public void length(int value){entityData.set(LENGTH,Math.max(1,Math.min(256,value)));}
 @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Length",length());}
 @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);length(tag.getInt("Length"));}
 @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(2,0,2).expandTowards(0,length(),0);}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
