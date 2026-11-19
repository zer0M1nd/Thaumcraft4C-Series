package fmmodern.client;

import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keep the original appearance for vanilla villagers that acquire the new workstation profession. */
@Mod.EventBusSubscriber(modid=fmmodern.ForbiddenMagic.ID,value=Dist.CLIENT)
public class HereticAppearance {
    private static HereticRenderer renderer;
    private static boolean rendering;
    @Mod.EventBusSubscriber(modid=fmmodern.ForbiddenMagic.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void layers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {renderer=new HereticRenderer(event.getContext());}
    }
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> event) {
        if(rendering || renderer==null || !(event.getEntity() instanceof Villager villager) || villager instanceof fmmodern.Heretic
                || villager.getVillagerData().getProfession()!=fmmodern.Content.HERETIC_PROFESSION.get())return;
        event.setCanceled(true);rendering=true;
        try {renderer.render(villager,net.minecraft.util.Mth.rotLerp(event.getPartialTick(),villager.yRotO,villager.getYRot()),
                event.getPartialTick(),event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());}
        finally{rendering=false;}
    }
}
