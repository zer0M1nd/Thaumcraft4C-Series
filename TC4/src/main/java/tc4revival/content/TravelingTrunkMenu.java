package tc4revival.content;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public  class TravelingTrunkMenu extends AbstractContainerMenu {
    private final TravelingTrunkEntity trunk;
    private final int rows;
    public TravelingTrunkMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        this(TCArcane.TRUNK_MENU.get(),id,inventory,openingEntity(inventory,buffer));
    }
    private static TravelingTrunkEntity openingEntity(Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        var tracked=inventory.player.level().getEntity(buffer.readInt()); int upgrade=buffer.readByte();
        var trunk=tracked instanceof TravelingTrunkEntity existing?existing:new TravelingTrunkEntity(TCEntities.TRAVELING_TRUNK.get(),inventory.player.level());
        trunk.upgrade(upgrade);
        if (!(tracked instanceof TravelingTrunkEntity)) trunk.discard();
        return trunk;
    }
    public TravelingTrunkMenu(MenuType<TravelingTrunkMenu> type,int id,Inventory inventory,TravelingTrunkEntity trunk) {
        super(type,id); this.trunk=trunk; rows=trunk.rows();
        for (int row=0;row<rows;row++) for (int col=0;col<9;col++) addSlot(new Slot(trunk.inventory,row*9+col,8+col*18,15+row*23));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,118+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,176));
        trunk.open(true); trunk.playSound(net.minecraft.sounds.SoundEvents.CHEST_OPEN,.5f,trunk.getRandom().nextFloat()*.1f+.9f);
    }
    public TravelingTrunkEntity trunk() { return trunk; }
    public int rows() { return rows; }
    @Override public boolean stillValid(Player player) { return trunk.isAlive() && rows==trunk.rows() && player.level()==trunk.level() && player.distanceToSqr(trunk)<=64 && trunk.mayOpen(player); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if (button!=1 || player.containerMenu!=this || !stillValid(player)) return false;
        trunk.stay(!trunk.stay()); return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (index<0 || index>=slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        var slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var source=slot.getItem(); var original=source.copy(); int size=rows*9;
        if (!(index<size?moveItemStackTo(source,size,slots.size(),true):moveItemStackTo(source,0,size,false))) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); return original;
    }
    @Override public void removed(Player player) { super.removed(player); trunk.open(false); trunk.playSound(net.minecraft.sounds.SoundEvents.CHEST_CLOSE,.5f,trunk.getRandom().nextFloat()*.1f+.9f); }
}
