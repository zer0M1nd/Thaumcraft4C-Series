package wgmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.FocusPouchItem;
import tc4revival.content.WandFocusItem;
import top.theillusivec4.curios.api.CuriosApi;

/** An equipped pouch is bound to its actual Curios slot and written through on every edit. */
public class EquippedPouchMenu extends AbstractContainerMenu {
 public final String slotName;public final int slotIndex;private final Player owner;private final ItemStack pouch;public final SimpleContainer contents=new SimpleContainer(18);
 public EquippedPouchMenu(int id,Inventory inventory,String name,int index){
  super(Content.EQUIPPED_POUCH_MENU.get(),id);owner=inventory.player;slotName=name;slotIndex=index;pouch=equipped();
  if(!owner.level().isClientSide){var saved=FocusPouchItem.contents(pouch);for(int i=0;i<18;i++)contents.setItem(i,saved.get(i));}
  contents.addListener(ignored->persist());
  for(int i=0;i<18;i++)addSlot(new Slot(contents,i,37+i%6*18,51+i/6*18){@Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof WandFocusItem;}@Override public int getMaxStackSize(){return 1;}});
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,151+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,209));
 }
 public ItemStack equipped(){return CuriosApi.getCuriosInventory(owner).resolve().flatMap(i->i.getStacksHandler(slotName)).map(h->slotIndex>=0&&slotIndex<h.getStacks().getSlots()?h.getStacks().getStackInSlot(slotIndex):ItemStack.EMPTY).orElse(ItemStack.EMPTY);}
 private void persist(){if(owner.level().isClientSide||!stillValid(owner))return;FocusPouchItem.save(pouch,java.util.stream.IntStream.range(0,18).mapToObj(contents::getItem).toList());owner.getInventory().setChanged();}
 @Override public boolean stillValid(Player player){return player==owner&&player.isAlive()&&equipped()==pouch&&pouch.getItem() instanceof FocusPouchItem;}
 @Override public void clicked(int slot,int button,ClickType type,Player player){if(!stillValid(player))return;super.clicked(slot,button,type,player);persist();}
 @Override public ItemStack quickMoveStack(Player player,int index){if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty()||!(stack.getItem() instanceof WandFocusItem))return ItemStack.EMPTY;var original=stack.copy();if(!(index<18?moveItemStackTo(stack,18,54,true):moveItemStackTo(stack,0,18,false)))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);persist();return original;}
 public static void open(net.minecraft.server.level.ServerPlayer player){if(!WGConfig.POUCH_ACTIVE.get()||!player.isAlive()||player.isSpectator())return;CuriosApi.getCuriosInventory(player).resolve().flatMap(i->i.findFirstCurio(s->s.getItem() instanceof FocusPouchItem)).ifPresent(slot->net.minecraftforge.network.NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new EquippedPouchMenu(id,inv,slot.slotContext().identifier(),slot.slotContext().index()),net.minecraft.network.chat.Component.translatable("item.thaumcraft.focus_pouch")),buf->{buf.writeUtf(slot.slotContext().identifier(),80);buf.writeVarInt(slot.slotContext().index());}));}
}
