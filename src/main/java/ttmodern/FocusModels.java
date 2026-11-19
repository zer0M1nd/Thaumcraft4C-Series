package ttmodern;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class FocusModels {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event){
        for(String name:java.util.List.of("smelting","flight","telekinesis","dislocation","shadowbeam"))event.register(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":item/focus_"+name+"_orn"));
        for(String name:java.util.List.of("ender_chest","healing","recall"))event.register(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":item/focus_"+name+"_depth"));
    }
}
