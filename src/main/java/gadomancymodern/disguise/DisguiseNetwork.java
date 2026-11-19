package gadomancymodern.disguise;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public class DisguiseNetwork {
    public static java.util.function.Consumer<Vec3> CLIENT_BUBBLES=point->{};
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(net.minecraft.resources.ResourceLocation.parse("gadomancy:disguise"),()->"1","1"::equals,"1"::equals);
    public record Bubbles(Vec3 point){}
    public static void register(){CHANNEL.messageBuilder(Bubbles.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->{buffer.writeDouble(message.point.x);buffer.writeDouble(message.point.y);buffer.writeDouble(message.point.z);}).decoder(buffer->new Bubbles(new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble()))).consumerMainThread((message,context)->{CLIENT_BUBBLES.accept(message.point);context.get().setPacketHandled(true);}).add();}
    public static void bubbles(ServerLevel level,Vec3 point){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(point.x,point.y,point.z,32,level.dimension())),new Bubbles(point));}
}
