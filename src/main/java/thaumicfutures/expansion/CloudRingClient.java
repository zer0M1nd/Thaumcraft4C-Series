package thaumicfutures.expansion;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT)
public final class CloudRingClient {
    private static boolean pressed;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        // Observe the key before LocalPlayer applies the ordinary ground jump.
        // At END that same first press already looks airborne and consumes the
        // ring's extra jump instead of waiting for a second press.
        if(event.phase!=TickEvent.Phase.START)return;
        var mc=Minecraft.getInstance();boolean now=mc.options.keyJump.isDown();
        if(mc.player!=null&&mc.screen==null&&now&&!pressed&&!mc.player.onGround()
                &&RestoredAccessories.worn(mc.player,RestoredContent.CLOUD_RING.get()).isPresent())
            ExpansionNetwork.CHANNEL.sendToServer(new ExpansionNetwork.CloudJump());
        pressed=now;
    }
    private CloudRingClient(){}
}
