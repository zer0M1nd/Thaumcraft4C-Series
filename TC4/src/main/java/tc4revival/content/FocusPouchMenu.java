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

/** Contents are written through to the held pouch, so moving/dying cannot duplicate a close-time snapshot. */
public  class FocusPouchMenu extends AbstractContainerMenu {
    private final ItemStack pouch;
    private final InteractionHand hand;
    private final int selected;
    private final Player player;
    private final SimpleContainer contents = new SimpleContainer(18);
    public FocusPouchMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, buffer.readEnum(InteractionHand.class)); }
    public FocusPouchMenu(int id, Inventory inventory, InteractionHand hand) {
        super(TCArcane.FOCUS_POUCH_MENU.get(), id); this.hand = hand; player = inventory.player; selected = inventory.selected; pouch = player.getItemInHand(hand);
        if (!player.level().isClientSide) {
            var saved = FocusPouchItem.contents(pouch); for (int i = 0; i < 18; i++) contents.setItem(i, saved.get(i));
        }
        contents.addListener(ignored -> persist());
        for (int i = 0; i < 18; i++) addSlot(new Slot(contents, i, 37 + i % 6 * 18, 51 + i / 6 * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof WandFocusItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 151 + row * 18));
        for (int col = 0; col < 9; col++) {
            boolean locked = hand == InteractionHand.MAIN_HAND && col == selected;
            addSlot(new Slot(inventory, col, 8 + col * 18, 209) {
                @Override public boolean mayPickup(Player player) { return !locked; }
                @Override public boolean mayPlace(ItemStack stack) { return !locked; }
            });
        }
    }
    private void persist() {
        if (player.level().isClientSide || !stillValid(player)) return;
        FocusPouchItem.save(pouch, java.util.stream.IntStream.range(0, 18).mapToObj(contents::getItem).toList());
        player.getInventory().setChanged();
    }
    public int lockedHotbarSlot() { return hand == InteractionHand.MAIN_HAND ? selected : -1; }
    @Override public boolean stillValid(Player player) { return player.isAlive() && player.getItemInHand(hand) == pouch && pouch.getItem() instanceof FocusPouchItem; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || hand == InteractionHand.MAIN_HAND && slot == 45 + selected) return;
        // Keep the open pouch in its hand; other hotbar/offhand swaps use vanilla slot rules.
        if (type == ClickType.SWAP && button == (hand == InteractionHand.MAIN_HAND ? selected : 40)) return;
        super.clicked(slot, button, type, player); persist();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || index == 45 + lockedHotbarSlot() && lockedHotbarSlot() >= 0) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof WandFocusItem)) return ItemStack.EMPTY;
        var original = stack.copy();
        if (!(index < 18 ? moveItemStackTo(stack, 18, 54, true) : moveItemStackTo(stack, 0, 18, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); persist(); return original;
    }
}
