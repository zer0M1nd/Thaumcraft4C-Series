package conciliummodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;

/** Original life-drain and initiation essentia trails, selected only by the server. */
public final class CrimsonDrainNetwork {
    private static final net.minecraftforge.network.simple.SimpleChannel NETWORK=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicconcilium:drain_fx"),()->"1","1"::equals,"1"::equals);
    public record Trail(ResourceLocation dimension,Vec3 from,Vec3 to,int phase,int count,int color,float scale){}
    public static void init(){NETWORK.messageBuilder(Trail.class,0,NetworkDirection.PLAY_TO_CLIENT)
        .encoder((p,b)->{b.writeResourceLocation(p.dimension);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeVarInt(p.phase);b.writeVarInt(p.count);b.writeInt(p.color);b.writeFloat(p.scale);})
        .decoder(b->new Trail(b.readResourceLocation(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readVarInt(),b.readInt(),b.readFloat()))
        .consumerMainThread((packet,context)->{var connection=context.get().getNetworkManager();net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->CrimsonDrainEffects.receive(packet,connection));context.get().setPacketHandled(true);}).add();}
    public static void drain(Entity receiver,Entity victim){trail(receiver.level(),victim.position(),receiver.position().add(0,.5,0),1,3,0x7a1a1a,.5f);}
    public static void initiation(Entity player,Entity convert){trail(player.level(),player.position().add(0,.5,0),convert.position().add(0,1.8,0),5,4,0x7a1a1a,.5f);}
    public static void trail(Level level,Vec3 from,Vec3 to,int phase,int count,int color,float scale){if(!level.isClientSide)NETWORK.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(from.x,from.y,from.z,64,level.dimension())),new Trail(level.dimension().location(),from,to,phase,count,color,scale));}
    private CrimsonDrainNetwork(){}
}
