package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** Contents are written through to the exact carrier, never copied back on close. */
public final class AstralMonitorMenu extends AbstractContainerMenu {
    public static final RegistryObject<MenuType<AstralMonitorMenu>> TYPE=ThaumaturgeMenu.MENUS.register("astral_monitor",()->net.minecraftforge.common.extensions.IForgeMenuType.create(AstralMonitorMenu::new));
    private final Player owner;
    private final ItemStack monitor;
    private final int carrierSlot;
    private final SimpleContainer contents;
    private boolean initialized;
    public AstralMonitorMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer){this(id,inventory,buffer.readVarInt());}
    public AstralMonitorMenu(int id,Inventory inventory,int carrierSlot){
        super(TYPE.get(),id);owner=inventory.player;this.carrierSlot=carrierSlot>=0&&carrierSlot<inventory.getContainerSize()?carrierSlot:inventory.selected;monitor=inventory.getItem(this.carrierSlot);
        contents=new SimpleContainer(1){
            @Override public void setChanged(){super.setChanged();if(initialized && !owner.level().isClientSide){monitor.getOrCreateTag().put("wand",getItem(0).save(new CompoundTag()));monitor.getTag().remove("vals");owner.getInventory().setChanged();}}
        };
        if(!owner.level().isClientSide)contents.setItem(0,AstralMonitor.stored(monitor));
        addSlot(new Slot(contents,0,78,30){@Override public boolean mayPlace(ItemStack stack){return AstralMonitor.accepts(stack);}@Override public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addInventorySlot(inventory,9+row*9+col,8+18*col,84+18*row);
        for(int col=0;col<9;col++)addInventorySlot(inventory,col,8+18*col,142);
        initialized=true;
    }
    private void addInventorySlot(Inventory inventory,int slot,int x,int y){addSlot(new Slot(inventory,slot,x,y){
        @Override public boolean mayPickup(Player player){return slot!=carrierSlot;}
        @Override public boolean mayPlace(ItemStack stack){return slot!=carrierSlot;}
    });}
    @Override public boolean stillValid(Player player){return player==owner && player.isAlive() && player.getInventory().getItem(carrierSlot)==monitor && monitor.getItem() instanceof AstralMonitor;}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!player.level().isClientSide && !stillValid(player))return;
        if(type==ClickType.SWAP && button==carrierSlot)return;
        if(slot>=0 && slot<slots.size() && slots.get(slot).container==player.getInventory() && slots.get(slot).getContainerSlot()==carrierSlot)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY;}
        else if(!AstralMonitor.accepts(stack) || !moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
