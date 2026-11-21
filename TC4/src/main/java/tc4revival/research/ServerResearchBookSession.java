package tc4revival.research;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Server-owned authorization for one book opening. Book item registration supplies the item predicate. */
public  class ServerResearchBookSession {
    public enum Rejection { NONE, CLOSED, CONTEXT, PLAYER, TOKEN, SEQUENCE }
    private final ServerPlayer owner;
    private final InteractionHand hand;
    private final int inventorySlot;
    private final int selectedSlot;
    private final ItemStack original;
    private final Predicate<ItemStack> allowedBook;
    private final String dimension;
    private final UUID token = UUID.randomUUID();
    private long nextSequence;
    private boolean closed;

    public ServerResearchBookSession(ServerPlayer owner, InteractionHand hand, Predicate<ItemStack> allowedBook) {
        this(owner,hand,-1,allowedBook);
    }
    public ServerResearchBookSession(ServerPlayer owner,int slot,Predicate<ItemStack> allowedBook) {
        this(owner,null,slot,allowedBook);
    }
    private ServerResearchBookSession(ServerPlayer owner,InteractionHand hand,int slot,Predicate<ItemStack> allowedBook) {
        this.owner = java.util.Objects.requireNonNull(owner);
        this.hand = hand; inventorySlot=slot;
        if(hand==null && (slot<0||slot>=owner.getInventory().getContainerSize()))throw new IllegalArgumentException("Book slot");
        this.allowedBook = java.util.Objects.requireNonNull(allowedBook);
        requireThread();
        selectedSlot = owner.getInventory().selected;
        original = currentBook().copy();
        dimension = owner.level().dimension().location().toString();
        if (!contextValid()) throw new IllegalStateException("Cannot open a research book in this context");
    }
    public UUID token() { return token; }
    public long nextSequence() { requireThread(); return nextSequence; }
    public void close() { requireThread(); closed = true; }
    public boolean valid() {
        requireThread();
        if (!closed && !contextValid()) closed = true;
        return !closed;
    }
    /** Consume each intent once, including gameplay requests subsequently rejected for costs or prerequisites. */
    public Rejection authorize(ServerPlayer sender, UUID suppliedToken, long sequence) {
        requireThread();
        if (closed) return Rejection.CLOSED;
        if (!contextValid()) { closed = true; return Rejection.CONTEXT; }
        if (sender != owner) return Rejection.PLAYER;
        if (!token.equals(suppliedToken)) return Rejection.TOKEN;
        if (sequence != nextSequence || sequence < 0 || nextSequence == Long.MAX_VALUE) return Rejection.SEQUENCE;
        nextSequence++;
        return Rejection.NONE;
    }
    private ItemStack currentBook() { return hand==null?owner.getInventory().getItem(inventorySlot):owner.getItemInHand(hand); }
    private boolean contextValid() {
        var current = currentBook();
        return owner.isAlive() && !owner.isRemoved() && !owner.isSpectator()
                && owner.containerMenu == owner.inventoryMenu
                && dimension.equals(owner.level().dimension().location().toString())
                && (hand != InteractionHand.MAIN_HAND || owner.getInventory().selected == selectedSlot)
                && !current.isEmpty() && allowedBook.test(current) && ItemStack.isSameItemSameTags(original, current);
    }
    private void requireThread() {
        if (owner.getServer() == null || !owner.getServer().isSameThread())
            throw new IllegalStateException("Book sessions must run on the owning server thread");
    }
}
