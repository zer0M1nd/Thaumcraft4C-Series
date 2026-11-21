package emtmodern.client;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ArcherRendering {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(emtmodern.ElectroMagicTools.ARCHER_ENTITY.get(),SnowGolemRenderer::new);}
}
