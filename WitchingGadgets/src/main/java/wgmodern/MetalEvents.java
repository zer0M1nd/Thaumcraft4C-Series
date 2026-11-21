package wgmodern;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="witchinggadgets")
public class MetalEvents {
 @SubscribeEvent public static void tags(TagsUpdatedEvent event){BlastRecipe.clearAutomatic();}
 @Mod.EventBusSubscriber(modid="witchinggadgets",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
 public static class Client {
  @SubscribeEvent public static void tags(TagsUpdatedEvent event){net.minecraft.client.Minecraft.getInstance().execute(ClusterColors::clear);}
 }
}
