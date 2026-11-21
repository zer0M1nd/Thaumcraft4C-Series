package gadomancymodern.node;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public class NodeVisualNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("gadomancy:node_visual"),()->"1","1"::equals,"1"::equals);
    public static class Effect {public final int kind,type;public final Vec3 from,to;public Effect(int kind,Vec3 from,Vec3 to,int type){this.kind=kind;this.from=from;this.to=to;this.type=type;}}
    public static void register(){CHANNEL.messageBuilder(Effect.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.kind);b.writeDouble(p.from.x);b.writeDouble(p.from.y);b.writeDouble(p.from.z);b.writeDouble(p.to.x);b.writeDouble(p.to.y);b.writeDouble(p.to.z);b.writeVarInt(p.type);}).decoder(b->new Effect(b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt())).consumerMainThread((p,c)->{gadomancymodern.client.NodeVisualEffects.receive(p);c.get().setPacketHandled(true);}).add();}
    public static void send(ServerLevel level,Effect effect){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(effect.to.x,effect.to.y,effect.to.z,32,level.dimension())),effect);}
    public static void bolt(ServerLevel level,Vec3 from,Vec3 to,int type){send(level,new Effect(0,from,to,type));}
    public static void absorb(ServerLevel level,BlockPos from,Vec3 to,int duration){send(level,new Effect(1,from.getCenter(),to,duration));}
    public static void burst(ServerLevel level,BlockPos pos){send(level,new Effect(2,pos.getCenter(),pos.getCenter(),0));}
    public static void runes(ServerLevel level,BlockPos pos,int color){send(level,new Effect(3,pos.getCenter(),pos.getCenter(),color));}
    public static void sparkles(ServerLevel level,BlockPos pos){send(level,new Effect(4,pos.getCenter(),pos.getCenter(),0));}
    public static void wandDrain(ServerLevel level,BlockPos pos,int color){send(level,new Effect(5,pos.getCenter().add(0,.3,0),pos.getCenter().add(0,.9+level.random.nextInt(4)/10d,0),color));}
}
