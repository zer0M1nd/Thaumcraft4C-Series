package tc4revival.research;

import java.util.Optional;
import java.util.UUID;
import tc4revival.network.BookActionPacket;
import tc4revival.network.BookResultPacket;

/** A screen owns its opening token; callbacks from a replaced screen cannot act on the new book. */
public  class BookClientState {
    private UUID token;
    private long next, pending = -1;
    private BookResultPacket result;
    public void open(UUID token) {
        this.token = java.util.Objects.requireNonNull(token); next = 0; pending = -1; result = null;
    }
    public Optional<UUID> token() { return Optional.ofNullable(token); }
    public boolean active(UUID owner) { return token != null && token.equals(owner); }
    public boolean waiting() { return pending >= 0; }
    public Optional<BookActionPacket> request(UUID owner, String key) {
        return request(owner,key,BookActionPacket.Action.REQUEST);
    }
    public Optional<BookActionPacket> request(UUID owner,String key,BookActionPacket.Action action) {
        if (!active(owner) || waiting() || next == Long.MAX_VALUE) return Optional.empty();
        var packet = new BookActionPacket(token, next, action, key);
        pending = next++; result = null;
        return Optional.of(packet);
    }
    public void receive(BookResultPacket packet) {
        if (!active(packet.token()) || packet.sequence() != pending) return;
        result = packet; pending = -1;
        if (packet.status() == BookResultPacket.Status.AUTH_REJECTED || packet.status() == BookResultPacket.Status.CLOSED) token = null;
    }
    public Optional<BookResultPacket> takeResult(UUID owner) {
        if (result == null || !result.token().equals(owner)) return Optional.empty();
        var answer = result; result = null; return Optional.of(answer);
    }
    public Optional<BookActionPacket> close(UUID owner) {
        if (!active(owner)) return Optional.empty();
        var packet = new BookActionPacket(token, next, BookActionPacket.Action.CLOSE, "");
        clear(); return Optional.of(packet);
    }
    public void clear() { token = null; next = 0; pending = -1; result = null; }
}
