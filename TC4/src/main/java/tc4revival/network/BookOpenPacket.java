package tc4revival.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/** Sent only after a server item handler has installed an authenticated opening. */
public record BookOpenPacket(UUID token, String research) {
    public BookOpenPacket(UUID token) { this(token, ""); }
    public BookOpenPacket { java.util.Objects.requireNonNull(token); java.util.Objects.requireNonNull(research); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUUID(token).writeUtf(research,128); }
    public static BookOpenPacket decode(FriendlyByteBuf buffer) {
        var result = new BookOpenPacket(buffer.readUUID(),buffer.readUtf(128));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing book opening data");
        return result;
    }
}
