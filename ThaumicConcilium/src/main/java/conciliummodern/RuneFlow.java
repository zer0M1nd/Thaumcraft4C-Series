package conciliummodern;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;

/** Cosmetic server-to-client message. Shield changes never depend on receipt of this packet. */
public final class RuneFlow {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:runes"),()->"1","1"::equals,"1"::equals);
    record Flight(net.minecraft.resources.ResourceLocation dimension,Vec3 from,Vec3 target){}
    record Shadow(net.minecraft.resources.ResourceLocation dimension,int caster,java.util.List<Vec3> points){}
    public static void init(){
        CHANNEL.messageBuilder(Flight.class,0,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b)->{b.writeResourceLocation(p.dimension);b.writeDouble(p.from.x).writeDouble(p.from.y).writeDouble(p.from.z);b.writeDouble(p.target.x).writeDouble(p.target.y).writeDouble(p.target.z);})
                .decoder(b->new Flight(b.readResourceLocation(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble())))
                .consumerMainThread((p,c)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->RuneFlowClient.receive(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Shadow.class,1,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b)->{b.writeResourceLocation(p.dimension);b.writeVarInt(p.caster);b.writeVarInt(p.points.size());for(var v:p.points)b.writeDouble(v.x).writeDouble(v.y).writeDouble(v.z);})
                .decoder(b->{var dimension=b.readResourceLocation();int caster=b.readVarInt(),size=b.readVarInt();if(size<0 || size>451)throw new IllegalArgumentException("Invalid shadow trail");var points=new java.util.ArrayList<Vec3>();for(int i=0;i<size;i++)points.add(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));return new Shadow(dimension,caster,points);})
                .consumerMainThread((p,c)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->RuneFlowClient.shadow(p));c.get().setPacketHandled(true);}).add();
    }
    public static void send(Entity caster,Entity target){
        CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(caster.getX(),caster.getY(),caster.getZ(),64,caster.level().dimension())),
                new Flight(caster.level().dimension().location(),caster.position(),target.position().add(0,1,0)));
    }
    public static void shadow(Entity caster,java.util.List<Vec3> points){
        CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(caster.getX(),caster.getY(),caster.getZ(),64,caster.level().dimension())),new Shadow(caster.level().dimension().location(),caster.getId(),java.util.List.copyOf(points)));
    }
    private RuneFlow(){}
}
