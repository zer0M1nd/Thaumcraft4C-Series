package tc4revival.content;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.aspect.*;

public  class DeconstructionTableMenu extends AbstractContainerMenu {
    private final DeconstructionTableBlockEntity table;
    private final Player viewer;
    private int time,aspectId;
    public DeconstructionTableMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data) {
        this(id,inventory,MenuBlockEntities.resolve(inventory,data,DeconstructionTableBlockEntity.class,TCArcane.DECONSTRUCTION_TABLE.get(),DeconstructionTableBlockEntity::new));
    }
    public DeconstructionTableMenu(int id,Inventory inventory,DeconstructionTableBlockEntity table) {
        super(TCArcane.DECONSTRUCTION_TABLE_MENU.get(),id); this.table=table; this.viewer=inventory.player;
        addSlot(new Slot(table,0,64,16) { @Override public boolean mayPlace(ItemStack stack) { return table.canPlaceItem(0,stack); } });
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142));
        boolean client=inventory.player.level().isClientSide;
        addDataSlot(new DataSlot() { public int get() { return client ? time : table.remaining(); } public void set(int value) { time=value; } });
        addDataSlot(new DataSlot() { public int get() { return client ? aspectId : table.pending()==null ? 0 : TCAspects.catalog().all().indexOf(table.pending())+1; } public void set(int value) { aspectId=value; } });
    }
    public DeconstructionTableBlockEntity table() { return table; }
    public int remaining() { return time; }
    public Aspect pending() { var all=TCAspects.catalog().all(); return aspectId>0 && aspectId<=all.size() ? all.get(aspectId-1) : null; }
    @Override public boolean stillValid(Player player) { return table.stillValid(player); }
    @Override public void broadcastChanges() {
        // Menus tick only while open. claim validates both the viewer and this exact table,
        // and consumes the pending reward before another viewer can receive it.
        if (viewer instanceof ServerPlayer server) table.claim(server);
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button) { return button==1 && player instanceof ServerPlayer server && table.claim(server); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var original=stack.copy();
        boolean moved=index==0 ? moveItemStackTo(stack,1,37,false) : slots.get(0).mayPlace(stack) ? moveItemStackTo(stack,0,1,false) : index<28 ? moveItemStackTo(stack,28,37,false) : moveItemStackTo(stack,1,28,false);
        if (!moved || stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return original;
    }
}
