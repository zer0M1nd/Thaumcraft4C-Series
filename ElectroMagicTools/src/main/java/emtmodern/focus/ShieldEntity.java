package emtmodern.focus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.*;

public class ShieldEntity extends Entity {
    protected static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(ShieldEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    protected java.lang.ref.WeakReference<Player> cachedOwner=new java.lang.ref.WeakReference<>(null);
    public ShieldEntity(EntityType<? extends ShieldEntity> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());}
    public void owner(Player player){entityData.set(OWNER,Optional.of(player.getUUID()));cachedOwner=new java.lang.ref.WeakReference<>(player);setPos(player.getX(),player.getY()+.5,player.getZ());}
    public Player owner(){var cached=cachedOwner.get();if(cached!=null&&!cached.isRemoved()&&cached.level()==level()&&ownedBy(cached))return cached;var resolved=entityData.get(OWNER).map(level()::getPlayerByUUID).orElse(null);cachedOwner=new java.lang.ref.WeakReference<>(resolved);return resolved;}
    public boolean ownedBy(Player player){return player!=null&&entityData.get(OWNER).filter(player.getUUID()::equals).isPresent();}
    @Override protected AABB makeBoundingBox(){return new AABB(getX()-2,getY()-2.5,getZ()-2,getX()+2,getY()+1.5,getZ()+2);}
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeCollidedWith(){return true;}
    @Override public boolean isPushable(){return true;}
    @Override public void push(Entity entity){if(!isPassengerOfSameVehicle(entity))entity.push((entity.getX()-getX())/5,0,(entity.getZ()-getZ())/5);}
    @Override public void tick(){super.tick();var owner=owner();if(owner==null){if(!level().isClientSide)discard();return;}if(!level().isClientSide){setPos(owner.getX(),owner.getY()+.5,owner.getZ());if(!owner.isUsingItem())discard();}}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return true;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
