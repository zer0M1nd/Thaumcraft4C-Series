package conciliummodern;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class LithographerMenu extends AbstractContainerMenu {
    public static final net.minecraftforge.registries.RegistryObject<MenuType<LithographerMenu>> TYPE=ThaumaturgeMenu.MENUS.register("lithographer",()->net.minecraftforge.common.extensions.IForgeMenuType.create(LithographerMenu::new));
    private final LithographerBlockEntity tile;
    public LithographerMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer){this(id,inventory,inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof LithographerBlockEntity value?value:null);}
    public LithographerMenu(int id,Inventory inventory,LithographerBlockEntity tile){
        super(TYPE.get(),id);this.tile=tile;
        net.minecraft.world.Container contents=tile==null?new net.minecraft.world.SimpleContainer(4):tile;
        int[][] positions={{52,30},{80,2},{108,30},{80,58}};
        for(int i=0;i<4;i++)addSlot(new Slot(contents,i,positions[i][0],positions[i][1]){
            @Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof AstralMonitor && AstralMonitor.accepts(AstralMonitor.stored(stack));}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+18*col,84+18*row));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+18*col,142));
    }
    @Override public boolean stillValid(Player player){return tile!=null && tile.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<4){if(!moveItemStackTo(stack,4,slots.size(),true))return ItemStack.EMPTY;}
        else if(!slots.get(0).mayPlace(stack)||!moveItemStackTo(stack,0,4,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
