package automagy.content;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="automagy")
public final class BrainReleaseEvents {
 @net.minecraftforge.eventbus.api.SubscribeEvent
 public static void click(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
  var level=event.getLevel();var player=event.getEntity();var pos=event.getPos();
  if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!player.isShiftKeyDown()||!event.getItemStack().isEmpty()||!player.mayBuild())return;
  if(level.getBlockEntity(pos) instanceof tc4revival.content.BrainJarBlockEntity brain&&level.hasChunkAt(pos.below())&&level.getBlockEntity(pos.below()) instanceof AmnesiacStoneBlockEntity stone){
   if(!level.isClientSide)stone.releaseBrain(brain);event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));
  }
 }
}
