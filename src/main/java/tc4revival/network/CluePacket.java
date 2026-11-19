package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;

/** A server-created clue notification; persistent knowledge is delivered by the revisioned knowledge stream. */
public record CluePacket(String research) {
    public CluePacket {
        if (research == null || !research.matches("[A-Za-z0-9_:./-]{1,128}")) throw new IllegalArgumentException("Invalid clue research key");
    }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(research, 128); }
    public static CluePacket decode(FriendlyByteBuf buffer) {
        var result = new CluePacket(buffer.readUtf(128));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing clue packet data");
        return result;
    }
}
