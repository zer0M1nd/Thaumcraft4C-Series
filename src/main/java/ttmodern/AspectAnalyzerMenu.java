package ttmodern;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AspectAnalyzerMenu extends AbstractContainerMenu {
    public final AspectAnalyzerEntity analyzer;
    public AspectAnalyzerMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data){this(id,inventory,(AspectAnalyzerEntity)inventory.player.level().getBlockEntity(data.readBlockPos()));}
    public AspectAnalyzerMenu(int id,Inventory inventory,AspectAnalyzerEntity analyzer){super(Tinkerer.ASPECT_ANALYZER_MENU.get(),id);this.analyzer=java.util.Objects.requireNonNull(analyzer);addSlot(new net.minecraftforge.items.SlotItemHandler(analyzer.items,0,20,30){@Override public boolean mayPlace(ItemStack stack){return thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(stack).size()>0;}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));}
    @Override public boolean stillValid(Player player){return analyzer.usable(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();if(!(index==0?moveItemStackTo(stack,1,37,true):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;}
}
