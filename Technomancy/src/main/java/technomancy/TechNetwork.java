package technomancy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TechNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Technomancy.id("main"),()->"1","1"::equals,"1"::equals);
    private record Power(CompoundTag data){}
    public static void register() {
        CHANNEL.messageBuilder(Power.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((packet,buf)->buf.writeNbt(packet.data())).decoder(buf->new Power(buf.readNbt()))
            .consumerMainThread((packet,context)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->technomancy.client.TechClient.power(packet.data()));context.get().setPacketHandled(true);}).add();
    }
    public static void sync(ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Power(PlayerPower.data(player).copy()));}
    private TechNetwork(){}
}
