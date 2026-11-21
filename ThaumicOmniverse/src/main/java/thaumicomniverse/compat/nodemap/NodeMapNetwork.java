package thaumicomniverse.compat.nodemap;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NodeMapNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "node_map"), () -> "1",
            version -> version.equals("1") || version.equals(NetworkRegistry.ABSENT),
            version -> version.equals("1") || version.equals(NetworkRegistry.ABSENT));
    public record Snapshot(boolean reset, ListTag rows) {}
    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> { buffer.writeBoolean(packet.reset()); var tag = new CompoundTag(); tag.put("nodes", packet.rows()); buffer.writeNbt(tag); })
                .decoder(buffer -> {
                    boolean reset = buffer.readBoolean(); var tag = buffer.readNbt();
                    var rows = tag == null ? new ListTag() : tag.getList("nodes", Tag.TAG_COMPOUND);
                    if (rows.size() > 64) throw new IllegalArgumentException("Oversized node map batch");
                    return new Snapshot(reset, rows);
                })
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> thaumicomniverse.client.NodeMapClient.receive(packet))).add();
    }
    public static void send(ServerPlayer player, boolean reset, ListTag rows) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(reset, rows));
    }
}
