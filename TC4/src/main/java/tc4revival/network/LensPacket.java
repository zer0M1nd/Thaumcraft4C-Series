package tc4revival.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Only an already-scanned visible target exposes aspects; modern-only objects retain an empty list. */
public record LensPacket(ResourceLocation dimension, Component name, List<Value> aspects, Component details) {
    public LensPacket(ResourceLocation dimension, Component name, List<Value> aspects) {
        this(dimension,name,aspects,Component.empty());
    }
    public record Value(String aspect, int amount) {}
    public LensPacket {
        java.util.Objects.requireNonNull(dimension);
        name = java.util.Objects.requireNonNull(name).copy();
        details = java.util.Objects.requireNonNull(details).copy();
        aspects = List.copyOf(aspects);
        if (aspects.size() > 1024 || aspects.stream().anyMatch(value -> !value.aspect().matches("[a-z0-9_:./-]{1,64}"))) {
            throw new IllegalArgumentException("Invalid lens aspects");
        }
    }
    @Override public Component name() { return name.copy(); }
    @Override public Component details() { return details.copy(); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension).writeComponent(name).writeVarInt(aspects.size());
        for (var value : aspects) buffer.writeUtf(value.aspect(), 64).writeInt(value.amount());
        buffer.writeComponent(details);
    }
    public static LensPacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        var name = buffer.readComponent();
        int count = buffer.readVarInt();
        if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid lens aspect count");
        var aspects = new ArrayList<Value>(count);
        for (int i = 0; i < count; i++) aspects.add(new Value(buffer.readUtf(64), buffer.readInt()));
        var details = buffer.readComponent();
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing lens data");
        return new LensPacket(dimension, name, aspects, details);
    }
}
