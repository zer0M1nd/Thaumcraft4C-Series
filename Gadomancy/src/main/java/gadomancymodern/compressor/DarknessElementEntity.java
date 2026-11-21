package gadomancymodern.compressor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.effect.*;
public class DarknessElementEntity extends ItemEntity {
    public static java.util.function.Consumer<DarknessElementEntity> CLIENT_EFFECTS=entity->{};
    public DarknessElementEntity(EntityType<? extends DarknessElementEntity> type,Level level){super(type,level);setUnlimitedLifetime();}
    @Override public void tick(){super.tick();if(!isAlive())return;if(level().isClientSide){CLIENT_EFFECTS.accept(this);return;}if((level().getGameTime()&15)==0)affect(level(),position());}
    public static void affect(Level level,Vec3 center){for(var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(center.subtract(.5,.5,.5),center.add(.5,.5,.5)).inflate(8)))living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0,true,true));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
