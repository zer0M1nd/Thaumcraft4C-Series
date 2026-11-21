package automagy.content;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="automagy")
public final class MirrorInteractionEvents {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void click(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity().isShiftKeyDown()&&event.getItemStack().getItem() instanceof tc4revival.content.CastingWandItem){
            var block=event.getLevel().getBlockState(event.getPos()).getBlock();
            if(block instanceof DeliveryMirrorBlock||block instanceof RequisitionTomeBlock)
                event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
    }
}
