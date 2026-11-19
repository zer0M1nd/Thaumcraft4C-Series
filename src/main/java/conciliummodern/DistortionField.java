package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Short refreshed lease prevents abandoned visual fields after logout or a denied cast. */
public final class DistortionField extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(DistortionField.class,EntityDataSerializers.OPTIONAL_UUID);
    private int lease;
    public DistortionField(EntityType<? extends DistortionField> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());}
    static void refresh(Player player,LivingEntity actor){
        var fields=actor.level().getEntitiesOfClass(DistortionField.class,actor.getBoundingBox().inflate(4),e->e.entityData.get(OWNER).filter(player.getUUID()::equals).isPresent());
        DistortionField field=fields.isEmpty()?new DistortionField(ConciliumEntities.DISTORTION_FIELD.get(),actor.level()):fields.get(0);
        field.entityData.set(OWNER,Optional.of(player.getUUID()));field.lease=3;field.setPos(actor.position());
        if(fields.isEmpty())actor.level().addFreshEntity(field);
    }
    @Override public void tick(){super.tick();if(!level().isClientSide && --lease<=0)discard();}
    @Override protected void readAdditionalSaveData(CompoundTag tag){lease=0;}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
