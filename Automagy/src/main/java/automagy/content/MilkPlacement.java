package automagy.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.*;

/** Sneak-use pours the vanilla milk bucket; ordinary use keeps drinking behavior. */
@Mod.EventBusSubscriber(modid="automagy")
public class MilkPlacement {
 @SubscribeEvent public static void pour(PlayerInteractEvent.RightClickBlock event){
  var player=event.getEntity();var stack=event.getItemStack();
  if(!player.isShiftKeyDown()||!stack.is(Items.MILK_BUCKET))return;
  var level=event.getLevel();var pos=event.getPos().relative(event.getFace());
  if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,event.getFace(),stack))return;
  if(level.isClientSide){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;}
  var result=FluidUtil.tryPlaceFluid(player,level,event.getHand(),pos,stack,new FluidStack(ForgeMod.MILK.get(),1000));
  if(result.isSuccess()){
   if(!player.getAbilities().instabuild)player.setItemInHand(event.getHand(),result.getResult());
   event.setCanceled(true);event.setCancellationResult(InteractionResult.CONSUME);
  }
 }
}
