package thaumicomniverse.expansion;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicomniverse",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class AncientForestClient {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.START
                &&net.minecraft.client.Minecraft.getInstance().level!=null
                &&thaumicomniverse.RevivalFeatures.ANCIENT_FOREST.get()
                &&tc4revival.config.TCConfig.CLIENT.blueMagicalForest.get()){
            tc4revival.config.TCConfig.CLIENT.blueMagicalForest.set(false);
            tc4revival.config.TCConfig.CLIENT.blueMagicalForest.save();
        }
    }
    private AncientForestClient(){}
}
