package arcanearteries;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;

/** Common settings load before recipes. The server snapshot never writes a client's local file. */
public final class ConfigSync {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ArcaneArteries.id("settings"), () -> "1", "1"::equals, "1"::equals);
    private static volatile CompoundTag remote = new CompoundTag();
    public record Packet(CompoundTag values) {}
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(Packet.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeNbt(packet.values()))
                .decoder(buffer -> new Packet(buffer.readNbt()))
                .consumerMainThread((packet, context) -> remote = packet.values() == null ? new CompoundTag() : packet.values().copy())
                .add());
    }
    static Object override(ForgeConfigSpec.ConfigValue<?> setting) {
        if (!net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()) return null;
        String key = String.join(".", setting.getPath());
        CompoundTag snapshot = remote;
        if (!snapshot.contains(key)) return null;
        Object defaultValue = setting.getDefault();
        return defaultValue instanceof Boolean ? snapshot.getBoolean(key)
                : defaultValue instanceof Integer ? snapshot.getInt(key) : snapshot.getString(key);
    }
    public static void clear() { remote = new CompoundTag(); }
    @SubscribeEvent public void sync(OnDatapackSyncEvent event) {
        CompoundTag snapshot = new CompoundTag();
        for (var setting : ArteriesConfig.settings()) {
            String key = String.join(".", setting.getPath());
            Object value = setting.get();
            if (value instanceof Boolean bool) snapshot.putBoolean(key, bool);
            else if (value instanceof Integer number) snapshot.putInt(key, number);
            else snapshot.putString(key, (String)value);
        }
        if (event.getPlayer() != null) CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), new Packet(snapshot));
        else for (var player : event.getPlayerList().getPlayers())
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Packet(snapshot));
    }
}
