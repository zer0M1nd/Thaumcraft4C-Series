package forgottenrelics;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class RelicsNetwork {
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ForgottenRelics.id("main"), () -> "1", "1"::equals, "1"::equals);

    private RelicsNetwork() {}

    public record Action(int action) {
        static void encode(Action value, FriendlyByteBuf buf) {
            buf.writeByte(value.action);
        }

        static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readUnsignedByte());
        }

        static void handle(Action message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(
                    () -> {
                        var player = context.getSender();
                        if (player == null
                                || !player.isAlive()
                                || player.isSpectator()
                                || player.containerMenu != player.inventoryMenu) return;
                        if (message.action == 0 && Relics.wearing(player, Relic.DISCORD_RING))
                            RelicUse.discord(player);
                        else if (message.action == 1) RelicUse.telekinesisAttack(player);
                    });
            context.setPacketHandled(true);
        }
    }

    public record Beam(Vec3 from, Vec3 to, int color) {
        static void encode(Beam value, FriendlyByteBuf buf) {
            buf.writeDouble(value.from.x);
            buf.writeDouble(value.from.y);
            buf.writeDouble(value.from.z);
            buf.writeDouble(value.to.x);
            buf.writeDouble(value.to.y);
            buf.writeDouble(value.to.z);
            buf.writeInt(value.color);
        }

        static Beam decode(FriendlyByteBuf buf) {
            return new Beam(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readInt());
        }

        static void handle(Beam message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(
                    () ->
                            DistExecutor.unsafeRunWhenOn(
                                    Dist.CLIENT,
                                    () -> () -> forgottenrelics.client.RelicsClient.beam(message)));
            context.setPacketHandled(true);
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Action::encode)
                .decoder(Action::decode)
                .consumerNetworkThread(Action::handle)
                .add();
        CHANNEL.messageBuilder(Beam.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Beam::encode)
                .decoder(Beam::decode)
                .consumerNetworkThread(Beam::handle)
                .add();
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(
                        () ->
                                new PacketDistributor.TargetPoint(
                                        from.x, from.y, from.z, 128, level.dimension())),
                new Beam(from, to, color));
    }
}
