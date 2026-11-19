package thmodern.alchemy;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
public final class AlchemiteEntity extends PrimedTnt {
 private java.util.UUID igniter;
 public AlchemiteEntity(EntityType<? extends AlchemiteEntity> type,Level level){super(type,level);}
 public void ignite(double x,double y,double z,LivingEntity owner){setPos(x,y,z);if(owner!=null)igniter=owner.getUUID();double angle=random.nextDouble()*Math.PI*2;setDeltaMovement(-Math.sin(angle)*.02,.2,-Math.cos(angle)*.02);setFuse(80);}
 @Override public LivingEntity getOwner(){return igniter!=null&&level() instanceof net.minecraft.server.level.ServerLevel server&&server.getEntity(igniter) instanceof LivingEntity living?living:null;}
 @Override protected void explode(){if(level() instanceof net.minecraft.server.level.ServerLevel server)new AlchemiteExplosion(server,this).detonate();}
 @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);if(igniter!=null)tag.putUUID("Igniter",igniter);}
 @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);igniter=tag.hasUUID("Igniter")?tag.getUUID("Igniter"):null;}
}
