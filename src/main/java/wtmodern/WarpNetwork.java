package wtmodern;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.function.Supplier;

public class WarpNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("warptheory","events"),()->"1","1"::equals,"1"::equals);
    public static void init(){
        CHANNEL.messageBuilder(Visual.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(Visual::write).decoder(Visual::read).consumerMainThread((msg,context)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->WarpClient.accept(msg));context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Heard.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((msg,buf)->{}).decoder(buf->new Heard()).consumerMainThread((msg,context)->{var p=context.get().getSender();if(p!=null&&WarpState.tag(p).getInt("ears")>0)WarpState.decrement(p,"ears",1);context.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Blink.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((msg,buf)->{buf.writeDouble(msg.x);buf.writeDouble(msg.y);buf.writeDouble(msg.z);}).decoder(buf->new Blink(buf.readDouble(),buf.readDouble(),buf.readDouble())).consumerMainThread((msg,context)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->WarpClient.blink(msg));context.get().setPacketHandled(true);}).add();
    }
    public static void send(ServerPlayer p,String action,BlockPos pos,int amount){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Visual(action,pos,amount));}
    public static void heard(){CHANNEL.sendToServer(new Heard());}
    public static void blink(ServerPlayer p){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(p.getX(),p.getY(),p.getZ(),128,p.level().dimension())),new Blink(p.getX(),p.getY(),p.getZ()));}
    public record Blink(double x,double y,double z){}
    public static class Heard{}
    public static class Visual {
        public final String action;public final BlockPos pos;public final int amount;
        public Visual(String action,BlockPos pos,int amount){this.action=action;this.pos=pos;this.amount=amount;}
        public void write(FriendlyByteBuf buf){buf.writeUtf(action,32);buf.writeBlockPos(pos);buf.writeVarInt(amount);}
        public static Visual read(FriendlyByteBuf buf){return new Visual(buf.readUtf(32),buf.readBlockPos(),buf.readVarInt());}
    }
}
