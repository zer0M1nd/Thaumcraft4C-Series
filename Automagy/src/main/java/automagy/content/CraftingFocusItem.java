package automagy.content;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.*;

public final class CraftingFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
 public static boolean has(ItemStack focus,String key){return focus.getItem() instanceof CraftingFocusItem item&&item.upgradeLevel(focus,FocusUpgrade.require("automagy:"+key).id)>0;}
 @Override public int color(ItemStack focus){return 0x4cc14c;}
 @Override public String sortingPrefix(){return "ACRAFT";}
 @Override public String depthTexture(){return "automagy:item/crafting_focus_depth";}
 @Override public String ornamentTexture(){return "automagy:item/crafting_focus_ornament_renderer";}
 @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
  consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
   private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
   @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
    if(renderer==null)renderer=new automagy.client.CraftingFocusOrnamentRenderer();return renderer;
   }
  });
 }
 @Override public Map<tc4revival.aspect.Aspect,Integer> costs(ItemStack focus){return has(focus,"gratis")?Map.of():Map.of(tc4revival.aspect.TCAspects.catalog().require("ordo"),700);}
 @Override public java.util.List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return java.util.stream.Stream.of("gratis","storage","transposing").filter(key->!has(focus,key)).map(key->FocusUpgrade.require("automagy:"+key)).toList();}
 @Override public boolean canApplyUpgrade(ItemStack focus,Player player,FocusUpgrade upgrade,int rank){return player instanceof net.minecraft.server.level.ServerPlayer server&&tc4revival.research.KnowledgeSavedData.get(server.server).snapshot(player.getUUID()).map(k->k.researchMarkers().contains("automagy:FOCUSCRAFTING_UPGRADE")).orElse(false);}
 @Override public void appendHoverText(ItemStack stack,Level level,List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags){
  super.appendHoverText(stack,level,lines,flags);
  if(has(stack,"storage"))lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.focus_stored",stack.hasTag()?stack.getTag().getCompound("AutomagyCrafting").getList("Items",10).size():0));
 }
 @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
  if(hand!=InteractionHand.MAIN_HAND||!(wand.getItem() instanceof CastingWandItem casting))return InteractionResultHolder.pass(wand);
  if(player instanceof net.minecraft.server.level.ServerPlayer server){
   if(!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu||player.getMainHandItem()!=wand||WandParts.sceptre(wand.getTag()))return InteractionResultHolder.fail(wand);
   var openingCost=costs(attached(wand));
   if(!openingCost.isEmpty()&&!casting.consumeForPlayer(wand,player,openingCost,false,false,true))return InteractionResultHolder.fail(wand);
   int slot=player.getInventory().selected;
   net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new PortableCraftingMenu(id,inv,slot),net.minecraft.network.chat.Component.translatable("item.automagy.crafting_focus")),buf->buf.writeVarInt(slot));
  }
  return InteractionResultHolder.sidedSuccess(wand,level.isClientSide);
 }
}
