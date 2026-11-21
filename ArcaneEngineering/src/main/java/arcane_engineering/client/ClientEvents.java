package arcane_engineering.client;

import arcane_engineering.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ArcaneEngineering.ID,value=Dist.CLIENT)
public class ClientEvents {
    private static boolean active, hovering;
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.getConnection()==null) { active=false; hovering=false; return; }
        boolean thrust=mc.screen==null && !mc.isPaused() && mc.options.keyJump.isDown() && mc.player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ImpulseBootsItem;
        boolean hover=mc.options.keyShift.isDown();
        if(thrust!=active || hover!=hovering || thrust && ++ticks%5==0) BootsNetwork.CHANNEL.sendToServer(new BootsNetwork.Controls(thrust,hover));
        active=thrust; hovering=hover;
    }
    @Mod.EventBusSubscriber(modid=ArcaneEngineering.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(ArcaneEngineering.DESTABILIZER_TILE.get(),DestabilizerRenderer::new); }
    }
}
