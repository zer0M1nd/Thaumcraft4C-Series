package ttmodern;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class WarpGateMenu extends AbstractContainerMenu {
    public final WarpGateEntity gate;
    public final boolean destinations;
    public WarpGateMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data){this(id,inventory,(WarpGateEntity)inventory.player.level().getBlockEntity(data.readBlockPos()),data.readBoolean());}
    public WarpGateMenu(int id,Inventory inventory,WarpGateEntity gate,boolean destinations){
        super(Tinkerer.WARP_GATE_MENU.get(),id);this.gate=java.util.Objects.requireNonNull(gate);this.destinations=destinations;
        for(int i=0;i<10;i++)addSlot(new net.minecraftforge.items.SlotItemHandler(gate.items,i,30+i%5*25,27+i/5*25){@Override public boolean isActive(){return !WarpGateMenu.this.destinations;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18){@Override public boolean isActive(){return !WarpGateMenu.this.destinations;}});
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142){@Override public boolean isActive(){return !WarpGateMenu.this.destinations;}});
        addDataSlot(new DataSlot(){public int get(){return gate.locked()?1:0;}public void set(int value){gate.locked(value!=0);}});
    }
    @Override public boolean stillValid(Player player){return gate.usable(player)&&(!destinations||gate.onGate(player));}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(!destinations&&stillValid(player))super.clicked(slot,button,type,player);}
    @Override public boolean clickMenuButton(Player player,int button){if(!stillValid(player))return false;if(!destinations&&button==0){gate.locked(!gate.locked());return true;}return destinations&&player instanceof net.minecraft.server.level.ServerPlayer server&&gate.teleport(server,button-100);}
    @Override public ItemStack quickMoveStack(Player player,int index){if(destinations||!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();if(!(index<10?moveItemStackTo(stack,10,46,true):stack.is(Tinkerer.SKY_PEARL.get())&&moveItemStackTo(stack,0,10,false)))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;}
}
