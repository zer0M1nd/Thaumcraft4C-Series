package conciliummodern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
/** Saved, finite warning and downward portable-hole front. */
public final class UpcomingHole extends Entity {
 private int age,remaining=33;private BlockPos front;
 public UpcomingHole(EntityType<? extends UpcomingHole> type,Level level){super(type,level);noPhysics=true;}
 @Override protected void defineSynchedData(){}
 @Override public void tick(){super.tick();age++;if(level().isClientSide){if(age<30)thaumcraft.api.fx.LegacyEffects.sparkle(level(),position().add(0,.1,0),0xa020f0,1,2,.3f);return;}if(age<30)return;
 if(!(level() instanceof ServerLevel level)||!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this)){discard();return;}if(front==null)front=blockPosition().below();if(remaining<=0||!level.hasChunkAt(front)||!level.getWorldBorder().isWithinBounds(front)){discard();return;}var actor=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);if(!tc4revival.content.PortableHoleBlockEntity.open(actor,front,1,1,120)){discard();return;}
 if(remaining>1)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){if(x==0&&z==0)continue;var pos=front.offset(x,0,z);if(level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos))tc4revival.content.PortableHoleBlockEntity.open(actor,pos,-1,1,120);}front=front.below();if(--remaining<=0)discard();}
 @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putInt("Age",age);tag.putInt("Remaining",remaining);if(front!=null)tag.putLong("Front",front.asLong());}
 @Override protected void readAdditionalSaveData(CompoundTag tag){age=Math.max(0,tag.getInt("Age"));remaining=net.minecraft.util.Mth.clamp(tag.getInt("Remaining"),0,33);front=tag.contains("Front")?BlockPos.of(tag.getLong("Front")):null;}
 @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
