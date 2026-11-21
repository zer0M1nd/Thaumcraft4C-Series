package automagy.content;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class FilterMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int ownerSlot;
    private final ItemStack owner;
    private final ItemFilter filter;
    private final SimpleContainer ghosts = new SimpleContainer(9);
    private final ContainerData data;
    public FilterMenu(int id, Inventory inventory, int ownerSlot) {
        super(Content.FILTER_MENU.get(), id); this.inventory = inventory; this.ownerSlot = ownerSlot;
        owner = ownerSlot >= 0 && ownerSlot < inventory.getContainerSize() ? inventory.getItem(ownerSlot) : ItemStack.EMPTY;
        filter = ItemFilter.load(owner);
        for (int i = 0; i < 9; i++) {
            ghosts.setItem(i, filter.entry(i));
            addSlot(new Slot(ghosts, i, 18+i%3*19,18+i/3*19) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        data = inventory.player.level().isClientSide ? new SimpleContainerData(12) : new ContainerData() {
            public int get(int i) { return i < 9 ? filter.count(i) : switch (i) { case 9 -> filter.useCount ? 1 : 0; case 10 -> filter.ignoreDamage ? 1 : 0; default -> filter.ignoreNbt ? 1 : 0; }; }
            public void set(int i, int value) {} public int getCount() { return 12; }
        }; addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 98 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 156));
    }
    public int ownerSlot(){return ownerSlot;}
    public boolean blacklist(){return owner.getItem() instanceof FilterItem f&&f.blacklist;}
    public int value(int i) { return data.get(i); }
    public String name() { return filter.name(); }
    public void initialName(String value) { filter.name(value); }
    private void save() {
        if (inventory.player.level().isClientSide) return;
        owner.getOrCreateTag().put("Filter", filter.save()); inventory.setChanged(); broadcastChanges();
    }
    public void setGhost(Player player,int slot,ItemStack value){
        if(player.level().isClientSide||!stillValid(player)||slot<0||slot>=9)return;
        filter.entry(slot,value.copyWithCount(1));ghosts.setItem(slot,filter.entry(slot));save();
    }
    public void setName(Player player, String name) { if (stillValid(player)) { filter.name(name); save(); } }
    @Override public boolean stillValid(Player player) {
        return player == inventory.player && owner.getItem() instanceof FilterItem
                && (inventory.getItem(ownerSlot) == owner || player.level().isClientSide && inventory.getItem(ownerSlot).is(owner.getItem()));
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button > 2) return false;
        switch (button) { case 0 -> filter.useCount = !filter.useCount; case 1 -> filter.ignoreDamage = !filter.ignoreDamage; case 2 -> filter.ignoreNbt = !filter.ignoreNbt; }
        save(); return true;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        // Editor never moves real items, including outside clicks, drag, hotbar swap and shift-click.
        if (slot < 0 || slot >= slots.size()) return;
        if (slot < 9 && type == ClickType.QUICK_MOVE) {
            filter.entry(slot, ItemStack.EMPTY); ghosts.setItem(slot, ItemStack.EMPTY); save();
        } else if (slot < 9 && type == ClickType.PICKUP && (button == 0 || button == 1)) {
            ItemStack cursor = getCarried();
            if (!cursor.isEmpty()) { filter.entry(slot, cursor); ghosts.setItem(slot, filter.entry(slot)); }
            else if (filter.useCount && !filter.entry(slot).isEmpty()) filter.count(slot, filter.count(slot) + (button == 0 ? 1 : -1));
            else { filter.entry(slot, ItemStack.EMPTY); ghosts.setItem(slot, ItemStack.EMPTY); }
            save();
        } else if (slot >= 9 && type == ClickType.PICKUP) {
            ItemStack source = slots.get(slot).getItem();
            if (source.isEmpty()) return;
            for (int i = 0; i < 9; i++) if (filter.entry(i).isEmpty()) { filter.entry(i, source); ghosts.setItem(i, filter.entry(i)); save(); break; }
        }
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
