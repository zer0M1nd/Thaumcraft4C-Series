package tc4revival.content;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Write through immediately: closing after death or moving the host cannot restore a stale jar. */
public  class HoverHarnessMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack harness;
    private final int selected;
    private final SimpleContainer contents = new SimpleContainer(1);
    public HoverHarnessMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readEnum(InteractionHand.class)); }
    public HoverHarnessMenu(int id, Inventory inventory, InteractionHand hand) {
        super(TCArcane.HOVER_HARNESS_MENU.get(), id);
        owner = inventory.player; this.hand = hand; selected = inventory.selected; harness = owner.getItemInHand(hand);
        if (!owner.level().isClientSide) contents.setItem(0, HoverFuel.jar(harness));
        contents.addListener(ignored -> persist());
        addSlot(new Slot(contents, 0, 80, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return HoverFuel.accepts(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) {
            boolean locked = hand == InteractionHand.MAIN_HAND && col == selected;
            addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
                @Override public boolean mayPickup(Player player) { return !locked; }
                @Override public boolean mayPlace(ItemStack stack) { return !locked; }
            });
        }
    }
    public int lockedHotbarSlot() { return hand == InteractionHand.MAIN_HAND ? selected : -1; }
    private void persist() {
        if (owner.level().isClientSide || !stillValid(owner)) return;
        HoverFuel.setJar(harness, contents.getItem(0)); owner.getInventory().setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && player.getItemInHand(hand) == harness && harness.getItem() instanceof HoverHarnessItem;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || type == ClickType.SWAP || lockedHotbarSlot() >= 0 && slot == 28 + selected) return;
        super.clicked(slot, button, type, player); persist();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || lockedHotbarSlot() >= 0 && index == 28 + selected) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem();
        if (stack.isEmpty() || !HoverFuel.accepts(stack)) return ItemStack.EMPTY;
        var original = stack.copy();
        if (!(index == 0 ? moveItemStackTo(stack, 1, 37, true) : moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); persist(); return original;
    }
}
