package wgmodern;
import net.minecraft.core.BlockPos;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.world.item.ItemStack;
public class LabelMenu extends AbstractContainerMenu {
 public final LabelLibrary.Tile tile;
 public LabelMenu(int id,Inventory inv,BlockPos pos){super(Content.LABEL_MENU.get(),id);tile=(LabelLibrary.Tile)inv.player.level().getBlockEntity(pos);addSlot(new Slot(tile,0,8,8){@Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof tc4revival.content.JarLabelItem;}});addSlot(new Slot(tile,1,8,51){@Override public boolean mayPlace(ItemStack s){return false;}@Override public void onTake(Player p,ItemStack s){tile.removeItem(0,1);super.onTake(p,s);}});for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,84+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));}
 @Override public boolean stillValid(Player p){return tile.stillValid(p);}
 @Override public boolean clickMenuButton(Player p,int index){var all=tc4revival.aspect.TCAspects.catalog().all();if(!(p instanceof net.minecraft.server.level.ServerPlayer server)||!stillValid(p)||index<0||index>=all.size())return false;var selected=all.get(index);if(!tc4revival.research.KnowledgeSavedData.get(server.server).snapshot(p.getUUID()).map(k->k.knows(selected)).orElse(false))return false;tile.aspect=selected.tag();tile.changed();broadcastChanges();return true;}
 @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
}
