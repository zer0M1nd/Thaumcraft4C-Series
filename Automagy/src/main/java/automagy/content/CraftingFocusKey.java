package automagy.content;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;

/** A key packet contains no slot or item data; the server resolves the currently held wand. */
public final class CraftingFocusKey {
 private static final Map<ServerPlayer,Long> LAST=new WeakHashMap<>();
 public static void press(ServerPlayer player){
  if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu)return;
  long now=player.level().getGameTime();if(LAST.containsKey(player)&&now-LAST.get(player)<5)return;LAST.put(player,now);
  var wand=player.getMainHandItem();if(!(wand.getItem() instanceof CastingWandItem)||WandParts.sceptre(wand.getTag()))return;
  var attached=WandFocusItem.attached(wand);
  if(!(attached.getItem() instanceof CraftingFocusItem)){
   var replacement=swapInventory(player,attached);if(replacement.isEmpty())return;
   WandFocusItem.attach(wand,replacement);player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
   player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.9f);
  }
  var focus=WandFocusItem.attached(wand);
  if(focus.getItem() instanceof CraftingFocusItem item)item.cast(wand,player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND,null);
 }
 private static ItemStack fromPouch(ItemStack pouch,ItemStack old){
  if(!(pouch.getItem() instanceof FocusPouchItem))return ItemStack.EMPTY;
  var inventory=FocusPouchItem.contents(pouch);
  for(int i=0;i<inventory.size();i++)if(CraftingFocusItem.has(inventory.get(i),"transposing")){
   var result=inventory.get(i).copy();inventory.set(i,old.copy());FocusPouchItem.save(pouch,inventory);return result;
  }return ItemStack.EMPTY;
 }
 private static ItemStack swapInventory(ServerPlayer player,ItemStack old){
  var inventory=player.getInventory();
  for(int i=0;i<inventory.getContainerSize();i++){
   var stack=inventory.getItem(i);
   if(CraftingFocusItem.has(stack,"transposing")){var result=stack.copy();inventory.setItem(i,old.copy());return result;}
   var result=fromPouch(stack,old);if(!result.isEmpty())return result;
  }
  var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve();
  if(curios.isPresent())for(var slots:curios.get().getCurios().values()){
   var stacks=slots.getStacks();for(int i=0;i<stacks.getSlots();i++){
    var copy=stacks.getStackInSlot(i).copy();var result=fromPouch(copy,old);
    if(!result.isEmpty()){stacks.setStackInSlot(i,copy);return result;}
   }
  }return ItemStack.EMPTY;
 }
 private CraftingFocusKey(){}
}
