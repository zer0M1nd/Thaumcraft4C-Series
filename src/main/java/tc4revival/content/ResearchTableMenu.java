package tc4revival.content;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import tc4revival.aspect.TCAspects;

public  class ResearchTableMenu extends AbstractContainerMenu {
    @Nullable private final ResearchTableBlockEntity table;
    private final BlockPos pos;
    private final Player viewer;
    private int clientPreferences;
    private long refillAt=-1,lastAutomationTick=-1,lastRandomTick=-100;
    private boolean automating;
    private tc4revival.research.ResearchNote completedPreview;
    public tc4revival.research.ResearchNote completedPreview() { return completedPreview; }
    @Override public void setItem(int slot,int stateId,ItemStack stack) {
        if(table==null && slot==1 && !stack.isEmpty())completedPreview=tc4revival.research.ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog()).note().filter(tc4revival.research.ResearchNote::complete).orElse(null);
        super.setItem(slot,stateId,stack);
    }

    public tc4revival.research.ResearchPreferences preferences() {
        if(table==null)return tc4revival.research.ResearchPreferences.unpack(clientPreferences);
        return tc4revival.research.KnowledgeSavedData.get(viewer.getServer()).snapshot(viewer.getUUID()).map(tc4revival.research.PlayerKnowledge::researchPreferences).orElse(tc4revival.research.ResearchPreferences.DEFAULT);
    }

    private final java.util.Set<String> clientBonuses = new java.util.LinkedHashSet<>();
    public ResearchTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, buffer.readBlockPos());
    }
    public ResearchTableMenu(int id, Inventory inventory, ResearchTableBlockEntity table) {
        this(id, inventory, table, table.getBlockPos());
    }
    private ResearchTableMenu(int id, Inventory inventory, @Nullable ResearchTableBlockEntity table, BlockPos pos) {
        super(TCTables.RESEARCH_MENU.get(), id);
        this.table = table; this.pos = pos;this.viewer=inventory.player;
        var contents = table == null ? new ItemStackHandler(2) : table.items;
        addSlot(new SlotItemHandler(contents, 0, 14, 10) {
            @Override public boolean mayPlace(ItemStack stack) { return ScribingToolsItem.accepts(stack); }
        });
        addSlot(new SlotItemHandler(contents, 1, 70, 10) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ResearchNotesItem; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, 9 + row * 9 + column, 48 + column * 18, 175 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 48 + column * 18, 233));
        addDataSlot(new DataSlot() {
            public int get() { return table==null?clientPreferences:preferences().packed(); }
            public void set(int value) { clientPreferences=value; }
        });
        for (var aspect : TCAspects.catalog().all()) addDataSlot(new DataSlot() {
            @Override public int get() { return (table == null ? clientBonuses : table.bonuses()).contains(aspect.tag()) ? 1 : 0; }
            @Override public void set(int value) { if (value == 0) clientBonuses.remove(aspect.tag()); else clientBonuses.add(aspect.tag()); }
        });
    }
    public BlockPos pos() { return pos; }
    @Nullable public ResearchTableBlockEntity table() { return table; }
    public java.util.Set<String> bonuses() { return table == null ? java.util.Set.copyOf(clientBonuses) : table.bonuses(); }
    @Override public boolean stillValid(Player player) { return table == null || table.valid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (!(index < 2 ? moveLegacyStack(stack, 2, slots.size(), true) : moveLegacyStack(stack, 0, 2, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    private boolean moveLegacyStack(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        int step = reverse ? -1 : 1;
        if (stack.isStackable()) for (int i = reverse ? end - 1 : start; i >= start && i < end && !stack.isEmpty(); i += step) {
            var destination = slots.get(i); var existing = destination.getItem();
            if (existing.isEmpty() || !destination.mayPlace(stack) || !ItemStack.isSameItemSameTags(existing, stack)) continue;
            int amount = Math.min(stack.getCount(), stack.getMaxStackSize() - existing.getCount());
            if (amount <= 0) continue;
            existing.grow(amount); stack.shrink(amount); destination.setChanged(); moved = true;
        }
        // TC4 copies the entire remainder into the first empty slot, including an oversized discovery stack.
        for (int i = reverse ? end - 1 : start; i >= start && i < end && !stack.isEmpty(); i += step) {
            var destination = slots.get(i);
            if (destination.hasItem() || !destination.mayPlace(stack)) continue;
            var placed = stack.copy();
            if (i < 2) placed.setCount(Math.min(64, placed.getCount())); // TileResearchTable's setter clamps to 64.
            destination.set(placed); destination.setChanged(); stack.setCount(0); return true;
        }
        return moved;
    }
    @Override public void broadcastChanges() {
        if(!automating && table!=null && viewer instanceof net.minecraft.server.level.ServerPlayer player && player.containerMenu==this && table.valid(player)) {
            long now=player.level().getGameTime();
            if(lastAutomationTick!=now) {
                lastAutomationTick=now;
                var noteStack=table.items.getStackInSlot(1);
                if(noteStack.is(TCItems.RESEARCH_NOTES.get())) {
                    var read=tc4revival.research.ResearchNoteNbt.read(noteStack.getTag(),TCAspects.catalog());
                    if(read.problems().isEmpty())read.note().ifPresent(note->{
                        var expanded=tc4revival.research.ResearchNotes.expandUndersized(note);
                        if(expanded!=note) {
                            var repaired=noteStack.copy();tc4revival.research.ResearchNoteNbt.write(repaired.getOrCreateTag(),expanded);
                            table.items.setStackInSlot(1,repaired);
                        }
                    });
                }
                if(preferences().autoRefill() && ResearchTableAutomation.unlocked(player)&&!tc4revival.compat.AutoResearchCompat.running(player)) {
                    automating=true;
                    try {
                        // Publish the completed board before consuming the actual item. The client keeps a read-only preview.
                        if(table.items.getStackInSlot(1).is(TCItems.DISCOVERY.get()))super.broadcastChanges();
                        if(ResearchTableAutomation.learn(player,table))refillAt=now+20;
                        if(table.items.getStackInSlot(1).isEmpty() && refillAt<0)refillAt=now+20;
                        if(refillAt>=0 && now>=refillAt) { ResearchTableAutomation.refill(player,table,this);refillAt=now+20; }
                        else if(refillAt<0 && now%20==0)ResearchTableAutomation.refill(player,table,this);
                    }finally { automating=false; }
                }else refillAt=-1;
            }
        }
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if(!(player instanceof net.minecraft.server.level.ServerPlayer server)||table==null||player.containerMenu!=this||!table.valid(player))return false;
        if(button==5)return table.act(server,ResearchTableBlockEntity.Action.DUPLICATE,0,0,null,null);
        var access=tc4revival.research.ResearchTableAccess.server(server);
        if(button>=20 && button<=23 && !access.sorting())return false;
        if(button==25) {
            long now=player.level().getGameTime();if(now-lastRandomTick<5)return false;lastRandomTick=now;
            return ResearchTableAutomation.random(server,table);
        }
        var old=preferences();tc4revival.research.ResearchPreferences next;
        if(button>=20 && button<=22)next=new tc4revival.research.ResearchPreferences(tc4revival.research.ResearchPreferences.Sort.values()[button-20],old.descending(),old.autoRefill());
        else if(button==23)next=new tc4revival.research.ResearchPreferences(old.sort(),!old.descending(),old.autoRefill());
        else if(button==24 && ResearchTableAutomation.unlocked(server))next=new tc4revival.research.ResearchPreferences(old.sort(),old.descending(),!old.autoRefill());
        else return false;
        tc4revival.research.KnowledgeSavedData.get(server.server).update(server.getUUID(),server.getRandom()::nextInt,k->{k.researchPreferences(next);return true;});
        broadcastChanges();return true;
    }
}
