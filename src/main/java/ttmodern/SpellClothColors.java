package ttmodern;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SpellClothColors {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,layer)->java.awt.Color.HSBtoRGB(.75f,Math.max(0,35-stack.getDamageValue())/35f*.5f,1),Tinkerer.SPELL_CLOTH.get());}
}
