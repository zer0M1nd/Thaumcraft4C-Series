package automagy.content;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
public final class SuppressedExperienceOrb extends ExperienceOrb implements net.minecraftforge.entity.IEntityAdditionalSpawnData {
 private int suppression;
 public SuppressedExperienceOrb(EntityType<? extends ExperienceOrb> type,Level level){super(type,level);}
 public SuppressedExperienceOrb(Level level,double x,double y,double z,int value,int suppression){this(Content.SUPPRESSED_ORB.get(),level);setPos(x,y,z);var data=new CompoundTag();data.putShort("Value",(short)value);data.putShort("Health",(short)5);data.putInt("Count",1);super.readAdditionalSaveData(data);this.suppression=Math.max(0,suppression);setDeltaMovement((random.nextDouble()*.2-.1)*2,random.nextDouble()*.4,(random.nextDouble()*.2-.1)*2);}
 @Override public void tick(){if(suppression<=0){super.tick();return;}suppression--;baseTick();var motion=getDeltaMovement();if(isInWater())motion=motion.multiply(.99,1,.99).add(0,motion.y<.06?.0005:0,0);else if(!isNoGravity())motion=motion.add(0,-.03,0);setDeltaMovement(motion);move(MoverType.SELF,motion);float friction=onGround()?.588f:.98f;setDeltaMovement(getDeltaMovement().multiply(friction,.98,friction));if(onGround())setDeltaMovement(getDeltaMovement().multiply(1,-.9,1));}
 @Override public void playerTouch(net.minecraft.world.entity.player.Player player){if(suppression<=0)super.playerTouch(player);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("AutomagySuppression",suppression);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);suppression=Math.max(0,Math.min(80,tag.getInt("AutomagySuppression")));}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
 @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buffer){buffer.writeInt(getValue());buffer.writeInt(suppression);}
 @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf buffer){var tag=new CompoundTag();tag.putShort("Value",(short)buffer.readInt());tag.putShort("Health",(short)5);tag.putInt("Count",1);tag.putInt("AutomagySuppression",buffer.readInt());readAdditionalSaveData(tag);}
}
