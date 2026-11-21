package tc4revival.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.research.ServerResearchBook;

public record BookResultPacket(UUID token, long sequence, Status status, String reason) {
    public enum Status { PURCHASED, PURCHASE_REJECTED, NOTE_CREATED, NOTE_EXISTING, NOTE_REJECTED, AUTH_REJECTED, CLOSED }
    public BookResultPacket {
        if (token == null || sequence < 0 || status == null || reason == null || !reason.matches("[A-Z_]{0,64}"))
            throw new IllegalArgumentException("Invalid book response");
    }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUUID(token).writeVarLong(sequence).writeEnum(status).writeUtf(reason, 64); }
    public static BookResultPacket decode(FriendlyByteBuf buffer) {
        var result = new BookResultPacket(buffer.readUUID(), buffer.readVarLong(), buffer.readEnum(Status.class), buffer.readUtf(64));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing book response data");
        return result;
    }
    public static BookResultPacket from(UUID token, long sequence, ServerResearchBook.Outcome outcome) {
        if (outcome instanceof ServerResearchBook.Denied denied) return new BookResultPacket(token, sequence, Status.AUTH_REJECTED, denied.reason().name());
        if (outcome instanceof ServerResearchBook.Purchase purchase) return new BookResultPacket(token, sequence,
                purchase.result().accepted() ? Status.PURCHASED : Status.PURCHASE_REJECTED,
                purchase.result().rejection().map(Enum::name).orElse(""));
        var note = ((ServerResearchBook.Note) outcome).result();
        return new BookResultPacket(token, sequence, switch (note.status()) {
            case CREATED -> Status.NOTE_CREATED;
            case EXISTING -> Status.NOTE_EXISTING;
            default -> Status.NOTE_REJECTED;
        }, note.status().name());
    }
}
