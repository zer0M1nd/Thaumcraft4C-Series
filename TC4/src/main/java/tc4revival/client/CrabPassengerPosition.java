package tc4revival.client;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CrabPassengerPosition {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||mc.level==null||mc.isPaused())return;
        // Local-player passengers are not guaranteed to follow the ordinary ClientLevel tick path.
        // Fix their actual client position and both histories, not just a renderer offset.
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof tc4revival.content.EldritchCrabEntity crab&&crab.isPassenger())crab.synchronizePassengerPosition();
    }
}
